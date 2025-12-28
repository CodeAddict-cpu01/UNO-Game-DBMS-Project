// --- File: src/UNOApp.java (Final Version) ---

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.Map;

public class UNOApp {

    public static void main(String[] args) {
        Connection conn = null;
        GameController gameController = new GameController();
        Scanner scanner = new Scanner(System.in);

        try {
            conn = DBConnector.getConnection();
            System.out.println("\n--- Setting up Players ---");
            List<Player> activePlayers = gameController.listAllPlayers(conn);
            
            if (activePlayers.size() >= 2) {
                System.out.println("\n--- Starting New Game ---");
                int gameId = gameController.startGame(conn, activePlayers);
                
                System.out.println("\n=============================================");
                System.out.println("   STARTING GAMEPLAY: AI LOGIC ENABLED");
                System.out.println("=============================================");
                
                // Game loop runs until someone wins or 50 turns pass (safety break)
                for(int turn = 1; turn <= 50; turn++) { 
                    
                    // A. SYNCHRONIZE STATE
                    GameStatus status = gameController.getGameStatus(conn, gameId);
                    int currentPlayerId = status.getCurrentTurnPlayerId();
                    Player currentPlayer = activePlayers.stream()
                                                       .filter(p -> p.getPlayerId() == currentPlayerId)
                                                       .findFirst().orElse(null);
                    if (currentPlayer == null) break;

                    boolean isHuman = currentPlayer.getType().equalsIgnoreCase("human");
                    int pendingDraws = status.getPendingDrawStack();

                    System.out.println("\n--- TURN " + turn + " ---");
                    
                    // Display Hand Counts
                    System.out.println("--- Current Card Counts ---");
                    Map<Integer, Integer> counts = gameController.getHandCounts(conn, gameId);
                    for (Player p : activePlayers) {
                        System.out.printf("  Player %d (%s): %d cards%n", 
                                          p.getPlayerId(), p.getName(), counts.getOrDefault(p.getPlayerId(), 0));
                    }
                    
                    System.out.printf("%nTOP CARD: %s | CURRENT PLAYER: %s (ID: %d)%n", 
                                       status.getTopCard().toString(), currentPlayer.getName(), currentPlayerId);
                    
                    if (pendingDraws > 0) {
                        System.out.printf("!!! PENDING DRAW STACK: %d cards !!!%n", pendingDraws);
                    }

                    // B. GET PLAYER'S HAND & DECISION PHASE 
                    List<Card> playerHand = gameController.getPlayerHand(conn, gameId, currentPlayerId);
                    
                    Card cardToPlay = null;
                    String nextColor = null; 

                    if (isHuman) {
                        // --- MANUAL INPUT LOGIC ---
                        System.out.println("Your Hand (ID: [Color Value]):");
                        for (Card card : playerHand) {
                            System.out.printf("  %d: [%s]%n", card.getCardId(), card.toString());
                        }

                        System.out.print("Enter Card ID to play (or press ENTER to draw/accept stack): ");
                        String input = scanner.nextLine().trim();

                        if (!input.isEmpty()) {
                            try {
                                int chosenId = Integer.parseInt(input);
                                cardToPlay = playerHand.stream()
                                                       .filter(card -> card.getCardId() == chosenId)
                                                       .findFirst()
                                                       .orElse(null);
                                
                                if (cardToPlay != null && !gameController.validateMove(cardToPlay, status.getTopCard(), pendingDraws)) {
                                    System.out.println("! INVALID MOVE: Cannot play that card. Drawing instead.");
                                    cardToPlay = null; 
                                }
                                if (cardToPlay != null && cardToPlay.isWild()) {
                                    System.out.print("Choose next color (red, green, blue, yellow): ");
                                    nextColor = scanner.nextLine().trim().toLowerCase();
                                    // Basic validation
                                    if (!List.of("red", "green", "blue", "yellow").contains(nextColor)) {
                                        nextColor = "red"; // Default
                                    }
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("! INVALID INPUT. Drawing instead.");
                                cardToPlay = null;
                            }
                        }
                        
                    } else { 
                        // --- ADVANCED AI/BOT LOGIC (NEW) ---
                        System.out.printf("Bot %d is thinking...", currentPlayerId);
                        Thread.sleep(1000); 

                        // 1. Get all valid moves
                        List<Card> validMoves = playerHand.stream()
                            .filter(card -> gameController.validateMove(card, status.getTopCard(), pendingDraws))
                            .collect(Collectors.toList());

                        if (validMoves.isEmpty()) {
                            cardToPlay = null; // AI must draw
                        } else {
                            // 2. Call the AI decision logic
                            AIDecision decision = gameController.getAIDecision(validMoves, playerHand);
                            cardToPlay = decision.card;
                            nextColor = decision.nextColor; // Will be null if not a wild
                        }
                    }

                    // --- EXECUTE TURN ---
                    if (cardToPlay != null) {
                        System.out.printf(">>> %s PLAYS: %s%n", currentPlayer.getName(), cardToPlay.toString());
                        if (cardToPlay.isWild()) {
                            System.out.printf(">>> %s changed the color to %s%n", currentPlayer.getName(), nextColor.toUpperCase());
                        }
                        
                        gameController.processMove(conn, gameId, currentPlayerId, cardToPlay, "played", nextColor);
                        
                        // Check for win AFTER playing
                        if(gameController.getPlayerHand(conn, gameId, currentPlayerId).isEmpty()){
                            System.out.println("!!! WINNER: Player " + currentPlayerId + " wins the game!");
                            break; // Exit the for-loop
                        }
                        
                    } else {
                        // Player must DRAW
                        if (pendingDraws == 0) {
                            System.out.printf(">>> %s draws 1 card and ends their turn.%n", currentPlayer.getName());
                            Card drawnCard = gameController.drawCard(conn, gameId, currentPlayerId);
                            gameController.processMove(conn, gameId, currentPlayerId, drawnCard, "drawn_and_passed", null);
                        } else {
                            System.out.printf(">>> %s must accept the stack of %d cards and ends their turn.%n", currentPlayer.getName(), pendingDraws);
                            Card dummyCard = playerHand.get(0); // Pass a dummy card just to log the action
                            gameController.processMove(conn, gameId, currentPlayerId, dummyCard, "drawn_and_passed", null);
                        }
                    }
                    
                    // Only pause if it's a human's turn next or the game is ending
                    System.out.print("\nPress ENTER to proceed to the next player...");
                    scanner.nextLine();
                }

                System.out.println("\n--- Demo Ended. Check the Moves table in MySQL for history! ---");
                
            } else {
                System.out.println("Not enough players to start a game (min 2 required).");
            }
            
        } catch (SQLException e) {
            System.err.println("A database error occurred during gameplay: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            DBConnector.closeConnection(conn);
            scanner.close();
        }
    }
}