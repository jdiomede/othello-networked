import java.net.*;
import java.io.*;
import java.util.Scanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.*;
import javax.xml.transform.*;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;

public class NetReversiClient extends JPanel implements MouseListener,ActionListener,Runnable{

	private static DocumentBuilderFactory factory = null;
	private static DocumentBuilder builder = null;
	private static Document document = null;
	private static Validator validator = null;
	private static SchemaFactory sfactory = null;
	private static Schema schema = null;
	private static Source schemaFile = null;
	private static TransformerFactory tFactory = null;
	private static Transformer transformer = null;
	private static URL url = null;
	private static String color = "wait";

	private Thread displayThread = null;
	private PollThread pollThread = null;
	private boolean isConnected = false;
	static final int MESSAGE = -1;

    private int id = 0;
    private int locX = -1;
    private int locY = -1;
    private Text text = null;
    private String currentTurnText = "waiting";
    private int currentTurn = WAIT;
    private boolean noMoreValidMovesBlack = false;
    private boolean noMoreValidMovesWhite = false;
    private boolean moveAgain = false;
    private boolean gameOver = false;

    private int height = 504;
    private int width = 504;
   	private int origX = 0;
	private int origY = 0;
	private int sizeX = 0;
   	private int sizeY = 0;

   	int black[][] = new int[8][8];
	int white[][] = new int[8][8];
	int returnMove[][] = new int[8][8];
    int validMoves[][][] = new int[8][8][8];

	static final int WAIT = 0;
    static final int WHITE = 1;
    static final int BLACK = 2;
    static final int OBSERVER = 3;
    static final int BLACKAGAIN = 4;
    static final int WHITEAGAIN = 5;
    static final int GAMEOVER = 6;

    private JButton connect = new JButton("connect");
    private JButton reset = new JButton("reset");

	private String userGram = "connect to server to begin!";
	private String myColorIs = "you do not have a color yet...";
    private JLabel result = new JLabel(userGram);
    private JLabel myColor = new JLabel(myColorIs);

    private String printWinner(){
		int countBlack = 0;
		int countWhite = 0;

		String returnValue = null;

		for (int x = 0; x < 8; x++){
			for (int y = 0; y < 8; y++){
				if(black[x][y] == 1){
					countBlack++;
				}
				if(white[x][y] == 1){
					countWhite++;
				}
			}
		}

		if(countBlack > countWhite){
			returnValue = "BLACK";
		}
		else if(countWhite > countBlack){
			returnValue = "WHITE";
		}
		else{
			returnValue = "stalemate";
		}

		return returnValue;
	}

    private void processInput(Move move) throws Exception{
    	if(!isConnected && (move != null)){
			if(move.getColor().equals("black")){
				isConnected = true;
				id = move.getID();
				color = "black";
				myColorIs = "your color is: black";
				userGram = "you are connected! waiting for an opponent to join...";

				//start waiting thread
				pollThread = new PollThread(this,url,id,color);
				pollThread.start();
				//pt.join();
			}
			else if(move.getColor().equals("white")){
				isConnected = true;
				id = move.getID();
				color = "white";
				currentTurnText = "black";
				currentTurn = BLACK;
				myColorIs = "your color is: white";
				userGram = "you are connected! waiting for opponent to move...";

				//start waiting thread
				pollThread = new PollThread(this,url,id,color);
				pollThread.start();
				//pt.join();
			}
			else if(move.getColor().equals("observer")){
				isConnected = true;
				id = move.getID();
				color = "white";
				myColorIs = "you do not have a color";
				userGram = "the game has started, you are too late...";

				//start observer thread
				//pollThread = new PollThread(this,url,id,color);
				//pollThread.start();
				//pt.join();
			}
    	}
    	else if(isConnected && (move != null)){
    		if(move.getColor().equals("black")){
    			currentTurnText = "black";
    			currentTurn = BLACK;
    			moveAgain = false;
    		}
    		else if(move.getColor().equals("white")){
    			currentTurnText = "white";
    			currentTurn = WHITE;
    			moveAgain = false;
    		}
    		else if(move.getColor().equals("gameover")){
    			currentTurnText = "gameover";
    			currentTurn = GAMEOVER;
    			moveAgain = false;
    		}
    		else if(move.getColor().equals("blackagain")){
    			currentTurnText = "black";
    			currentTurn = BLACK;
    			moveAgain = true;
			}
			else if(move.getColor().equals("whiteagain")){
				currentTurnText = "white";
    			currentTurn = WHITE;
				moveAgain = true;
			}

			if(currentTurn == GAMEOVER){
				int makeMyMove = BLACK;
				if(color.equals("white")){
					makeMyMove = WHITE;
				}

				int returnValue = makeMove(move.getLocationX(),move.getLocationY(),makeMyMove);
				gameOver = true;

				String winner = printWinner();

				userGram = "game over! "+winner+" has won.";
			}
			else if(currentTurnText.equals(color)){
				int makeMyMove = BLACK;
				if(color.equals("white")){
					makeMyMove = WHITE;
				}

				if(moveAgain){
					int returnValue = makeMove(move.getLocationX(),move.getLocationY(),makeMyMove);

					System.out.println();
					System.out.println("local next: "+returnValue+", server next: "+currentTurnText);
					System.out.println();

					//userGram = "you have no valid moves, waiting for opponent to move again...";
					userGram = "move accepted, opponent has no valid moves, please move again...";

					//start waiting thread
					//PollThread pt = new PollThread(this,url,id,color);
					//pt.start();
				}
				else{
					userGram = "invalid move, please try again...";
				}
			}
			else if(!currentTurnText.equals(color)){
				int makeMyMove = BLACK;
				if(color.equals("white")){
					makeMyMove = WHITE;
				}
				int returnValue = makeMove(move.getLocationX(),move.getLocationY(),makeMyMove);

				System.out.println();
				System.out.println("local next: "+returnValue+", server next: "+currentTurnText);
				System.out.println();

				if(moveAgain){
					//userGram = "move accepted, opponent has no valid moves, please move again...";
					userGram = "you have no valid moves, waiting for opponent to move again...";
				}
				else{
					userGram = "move accepted, waiting for opponent to move...";
				}

				//start waiting thread
				pollThread = new PollThread(this,url,id,color);
				pollThread.start();
			}
    	}
    }

    private Move unMarshalInput(String xmlString) throws Exception{

		//System.out.println(xmlString);

        int id;
        int moveX;
        int moveY;
		String color;

	    try{
            document = builder.parse(new org.xml.sax.InputSource(new StringReader(xmlString)));
        }catch(Exception e){}

		try {
		    validator.validate(new DOMSource(document));
		} catch (SAXException se) {
		    System.out.println(se);
		}

		//now that we have our org.w3c.Document we start by getting root element
		Element root = document.getDocumentElement();
		id = Integer.parseInt(root.getAttribute("id"));

		Element locElementX = (Element) document.getElementsByTagName("locationX").item(0);
	    Element locElementY = (Element) document.getElementsByTagName("locationY").item(0);
		Element colorElement = (Element) document.getElementsByTagName("color").item(0);

		moveX = Integer.parseInt(locElementX.getFirstChild().getNodeValue());
	    moveY = Integer.parseInt(locElementY.getFirstChild().getNodeValue());
		color = colorElement.getFirstChild().getNodeValue();

		Move move = new Move(id,color,moveX,moveY);

		return move;
    }

    private static String marshalMoveToXML(int moveX, int moveY) throws Exception{

    	Element locElementX = (Element) document.getElementsByTagName("locationX").item(0);
	    Element locElementY = (Element) document.getElementsByTagName("locationY").item(0);
	    Element colorElement = (Element) document.getElementsByTagName("color").item(0);

	    locElementX.getFirstChild().setNodeValue(Integer.toString(moveX));
	    locElementY.getFirstChild().setNodeValue(Integer.toString(moveY));
	    colorElement.getFirstChild().setNodeValue(color);

		StringWriter sw = new StringWriter();

		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(sw);
		transformer.transform(source, result);

		//System.out.println("marshal: "+sw.toString());
		return sw.toString();
    }

    private static String postToServlet(String xmlMove) throws Exception{

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "text/xml");
		conn.setRequestProperty("Content-Language", "en-US");
		conn.setDoInput(true);
		conn.setDoOutput(true);

		xmlMove = xmlMove.replace(' ','+');

		System.out.println("request: "+xmlMove);
		System.out.println();
		PrintWriter pw = new PrintWriter(conn.getOutputStream());
		pw.write(xmlMove);
		pw.close();

		Scanner in = new Scanner(conn.getInputStream());
		StringBuffer retVal = new StringBuffer();
		while (in.hasNext())
		    retVal.append(in.next());
		conn.disconnect();

		System.out.println("response: "+retVal.toString());
		System.out.println();
		return (retVal.toString());
    }

    private void findStreak(int x, int y, int direction, int colorForMove){
        int myMoves[][] = new int[8][8];
        int opMoves[][] = new int[8][8];
        switch(colorForMove){
            case BLACK:
                myMoves = black;
                opMoves = white;
            break;
            case WHITE:
                myMoves = white;
                opMoves = black;
            break;
        }

        boolean boardBoundaryReached = false;
        if(x<0 || y<0 || x>7 || y>7){
            boardBoundaryReached = true;
        }
        while(!boardBoundaryReached && opMoves[x][y] == 1){
            switch(direction){
                case 0:
                    //northwest
                    x--;
                    y--;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
                case 1:
                    //north
                    y--;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
                case 2:
                    //northeast
                    x++;
                    y--;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
                case 3:
                    //west
                    x--;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
                case 4:
                    //east
                    x++;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
                case 5:
                    //southwest
                    x--;
                    y++;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
                case 6:
                    //south
                    y++;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
                case 7:
                    //southeast
                    x++;
                    y++;
                    if(x<0 || y<0 || x>7 || y>7){
                        boardBoundaryReached = true;
                    }
                break;
            }
        }

        if(!boardBoundaryReached && myMoves[x][y] != 1 && opMoves[x][y] != 1){
            validMoves[x][y][direction] = 1;
            //System.out.println(x+" "+y);
        }
    }

    private void findValidMoves(int colorForMove){
        int myMoves[][] = new int[8][8];
        int opMoves[][] = new int[8][8];
        switch(colorForMove){
            case BLACK:
                myMoves = black;
                opMoves = white;
            break;
            case WHITE:
                myMoves = white;
                opMoves = black;
            break;
        }

        //clear array
        for(int a = 0; a < 8; a++){
            for(int b = 0; b < 8; b++){
                for(int c = 0; c < 8; c++){
                    validMoves[a][b][c] = 0;
                }
            }
        }

        for (int x = 0; x < 8; x++){
            for (int y = 0; y < 8; y++){
                if(myMoves[x][y] == 1){
                    for (int i = 0; i < 8; i++){
                        switch(i){
                            case 0:
                                //northwest
                                if(((x-1)>0 && (y-1)>0) && opMoves[x-1][y-1] == 1){
                                    findStreak(x-1,y-1,i,colorForMove);
                                }
                            break;
                            case 1:
                                //north
                                if(((y-1)>0) && opMoves[x][y-1] == 1){
                                    findStreak(x,y-1,i,colorForMove);
                                }
                            break;
                            case 2:
                                //northeast
                                if(((x+1)<8 && (y-1)>0) && opMoves[x+1][y-1] == 1){
                                    findStreak(x+1,y-1,i,colorForMove);
                                }
                            break;
                            case 3:
                                //west
                                if(((x-1)>0) && opMoves[x-1][y] == 1){
                                    findStreak(x-1,y,i,colorForMove);
                                }
                            break;
                            case 4:
                                //east
                                if(((x+1)<8) && opMoves[x+1][y] == 1){
                                    findStreak(x+1,y,i,colorForMove);
                                }
                            break;
                            case 5:
                                //southwest
                                if(((x-1)>0 && (y+1)<8) && opMoves[x-1][y+1] == 1){
                                    findStreak(x-1,y+1,i,colorForMove);
                                }
                            break;
                            case 6:
                                //south
                                if(((y+1)<8) && opMoves[x][y+1] == 1){
                                    findStreak(x,y+1,i,colorForMove);
                                }
                            break;
                            case 7:
                                //southeast
                                if(((x+1)<8 && (y+1)<8) && opMoves[x+1][y+1] == 1){
                                    findStreak(x+1,y+1,i,colorForMove);
                                }
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean checkMove(int movx, int movy, int colorForMove){
        boolean returnValue = false;

        int myMoves[][] = new int[8][8];
        int opMoves[][] = new int[8][8];
        switch(colorForMove){
            case BLACK:
                myMoves = black;
                opMoves = white;
            break;
            case WHITE:
                myMoves = white;
                opMoves = black;
            break;
        }

        //calculate valid moves
        //System.out.println("valid moves for "+color+":");
        findValidMoves(colorForMove);

        //check if move is valid
        for(int d = 0; d < 8; d++){
            if(validMoves[movx][movy][d] == 1){
                myMoves[movx][movy] = 1;
                //flip opponents squares
                int sx = movx;
                int sy = movy;
                switch(d){
                    case 0:
                        //southeast
                        sx++;
                        sy++;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sx++;
                            sy++;
                        }
                    break;
                    case 1:
                        //south
                        sy++;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sy++;
                        }
                    break;
                    case 2:
                        //southwest
                        sx--;
                        sy++;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sx--;
                            sy++;
                        }
                    break;
                    case 3:
                        //east
                        sx++;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sx++;
                        }
                    break;
                    case 4:
                        //west
                        sx--;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sx--;
                        }
                    break;
                    case 5:
                        //northeast
                        sx++;
                        sy--;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sx++;
                            sy--;
                        }
                    break;
                    case 6:
                        //north
                        sy--;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sy--;
                        }
                    break;
                    case 7:
                        //northwest
                        sx--;
                        sy--;
                        while(opMoves[sx][sy] == 1){
                            opMoves[sx][sy] = 0;
                            myMoves[sx][sy] = 1;
                            sx--;
                            sy--;
                        }
                    break;
                }
                switch(colorForMove){
                    case BLACK:
                        black = myMoves;
                        white = opMoves;
                    break;
                    case WHITE:
                        white = myMoves;
                        black = opMoves;
                    break;
                }
                returnValue = true;
            }
        }

    	return returnValue;
    }

    public int makeMove(int x, int y, int colorForMove){
        /*switch(currentTurn){
            case BLACKAGAIN:
                currentTurn = BLACK;
            break;
            case WHITEAGAIN:
                currentTurn = WHITE;
            break;
        }*/
        /*if(colorForMove == currentTurn)*/{
            if(checkMove(x,y,colorForMove)){
                switch(colorForMove){
                    case BLACK:
                        //currentTurn = WHITE;

                        //check if there are still valid moves
                        noMoreValidMovesWhite = true;
                        findValidMoves(WHITE);
                        for(int a = 0; a < 8; a++){
                            for(int b = 0; b < 8; b++){
                                for(int c = 0; c < 8; c++){
                                    if(validMoves[a][b][c] == 1){
                                        noMoreValidMovesWhite = false;
                                    }
                                }
                            }
                        }
                        noMoreValidMovesBlack = true;
                        findValidMoves(BLACK);
                        for(int a = 0; a < 8; a++){
                            for(int b = 0; b < 8; b++){
                                for(int c = 0; c < 8; c++){
                                    if(validMoves[a][b][c] == 1){
                                        noMoreValidMovesBlack = false;
                                    }
                                }
                            }
                        }

                        if(noMoreValidMovesWhite){
                            if(noMoreValidMovesBlack){
                                //game over
                                gameOver = true;
                                currentTurn = GAMEOVER;
                                //opponentWhite.pushMove(GAMEOVER);
                            }
                            else{
                                //this opponent cannot move, turn is passed
                                //currentTurn = BLACKAGAIN;
                                //opponentWhite.pushMove(BLACK);
                            }
                        }
                        else{
                            //opponentWhite.pushMove(WHITE);
                        }
                    break;
                    case WHITE:
                        //currentTurn = BLACK;

                        //check if there are still valid moves
                        noMoreValidMovesBlack = true;
                        findValidMoves(BLACK);
                        for(int a = 0; a < 8; a++){
                            for(int b = 0; b < 8; b++){
                                for(int c = 0; c < 8; c++){
                                    if(validMoves[a][b][c] == 1){
                                        noMoreValidMovesBlack = false;
                                    }
                                }
                            }
                        }
                        noMoreValidMovesWhite = true;
                        findValidMoves(WHITE);
                        for(int a = 0; a < 8; a++){
                            for(int b = 0; b < 8; b++){
                                for(int c = 0; c < 8; c++){
                                    if(validMoves[a][b][c] == 1){
                                        noMoreValidMovesWhite = false;
                                    }
                                }
                            }
                        }

                        if(noMoreValidMovesBlack){
                            if(noMoreValidMovesWhite){
                                //game over
                                gameOver = true;
                                currentTurn = GAMEOVER;
                                //opponentBlack.pushMove(GAMEOVER);
                            }
                            else{
                                //this opponent cannot move, turn is passed
                                //currentTurn = WHITEAGAIN;
                                //opponentBlack.pushMove(WHITE);
                            }
                        }
                        else{
                            //opponentBlack.pushMove(BLACK);
                        }
                    break;
                }
                //notify observer(s) of new move
                /*for (int obv = 0; obv < observersList.size(); obv++){
                    ReversiClientInterface temp = observersList.get(obv);
                    if(gameOver){
                        temp.pushMove(GAMEOVER);
                    }
                    else{
                        temp.pushMove(OBSERVER);
                    }
                }*/
            }
        }

        return currentTurn;
    }

	public void actionPerformed(ActionEvent ae){
		if (ae.getSource() == connect){
			if(!isConnected){
				String xmlMove = null;
				String reply = null;

				if(displayThread == null){
					displayThread = new Thread(this);
					displayThread.start();
				}

				try{
					xmlMove = marshalMoveToXML(MESSAGE,MESSAGE);
				}catch(Exception e2){
					userGram = "connection failed, please try again...";
					System.out.println(e2);
				}

				try{
					reply = postToServlet(xmlMove);
				}catch(Exception e2){
					userGram = "connection failed, please try again...";
					System.out.println(e2);
				}

				if(reply != null){
					Move move = null;

					reply = reply.replace('+',' ');

					try{
					    move = unMarshalInput(reply);
					}catch(Exception e){}

					if(move != null){
						try{
						    processInput(move);
						}catch(Exception e){}
					}
					else{
						userGram = "server response failed, please try again...";
					}
				}
				else{
					userGram = "connection failed, please try again...";
				}
			}
		}
		else if (ae.getSource() == reset){
			String xmlMove = null;
			String reply = null;

			Element locElementX = (Element) document.getElementsByTagName("locationX").item(0);
		    Element locElementY = (Element) document.getElementsByTagName("locationY").item(0);
		    Element colorElement = (Element) document.getElementsByTagName("color").item(0);

		    locElementX.getFirstChild().setNodeValue(Integer.toString(-1));
		    locElementY.getFirstChild().setNodeValue(Integer.toString(-1));
		    colorElement.getFirstChild().setNodeValue("reset");

			StringWriter sw = new StringWriter();

			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(sw);
			try{
				transformer.transform(source, result);
			}catch(TransformerException te){}

			xmlMove = sw.toString();

			try{
				reply = postToServlet(xmlMove);
			}catch(Exception e2){
				userGram = "connection failed, please try again...";
				System.out.println(e2);
			}

			userGram = "reset! please connect again to play.";
			if(isConnected){
				reset();
			}
		}
	}

	public void mouseClicked(MouseEvent e){
		if(isConnected && currentTurnText.equals(color)){
			int rawX = e.getX()-origX;
			int rawY = e.getY()-origY;

			int moveX = 0;
			int moveY = 0;

			String xmlMove = null;
			String reply = null;

			//implement rough error checking here
			// all moves will at least be within bounds
			boolean outOfBounds = false;
			if(rawX >= width || rawX < 0){
				outOfBounds = true;
			}

			if(rawY >= height || rawY < 0){
				outOfBounds = true;
			}

			if(!outOfBounds){
				moveX = (int)Math.floor((e.getX()-origX)/sizeX);
				moveY = (int)Math.floor((e.getY()-origY)/sizeY);

				try{
					xmlMove = marshalMoveToXML(moveX,moveY);
				}catch(Exception e2){
					userGram = "parse move failed, please try again...";
				}

				try{
					reply = postToServlet(xmlMove);
				}catch(Exception e2){
					userGram = "post move failed, please try again...";
				}
			}

			if(reply != null){
				Move move = null;

				reply = reply.replace('+',' ');

				System.out.println("click: "+reply);

				try{
				    move = unMarshalInput(reply);
				}catch(Exception e2){}

				if(move != null){
					try{
					    processInput(move);
					}catch(Exception e2){}
				}
				else{
					userGram = "server response error, please try again...";
				}
			}
			else{
				//userGram = "move failed, please try again...";
			}
		}
	}
 	public void mouseEntered(MouseEvent e){}
 	public void mouseExited(MouseEvent e){}
 	public void mousePressed(MouseEvent e){}
 	public void mouseReleased(MouseEvent e){}

 	public void printBoard(Graphics chamber) throws Exception{

    	for (int x = 0; x < 8; x++){
    		for (int y = 0; y < 8; y++){
    			if (white[x][y] != 0) {
    				//System.out.print("0");
    				chamber.setColor(Color.WHITE);
					chamber.fillArc(x*sizeX+origX,y*sizeY+origY,sizeX,sizeY, 0, 360);
    			}
    			else if (black[x][y] != 0){
    				//System.out.print("X");
    				chamber.setColor(Color.BLACK);
					chamber.fillArc(x*sizeX+origX,y*sizeY+origY,sizeX,sizeY, 0, 360);
    			}
    		}
    	}
    }

    private void reset(){

    	if(pollThread != null)
    		pollThread.waiting = false;

	    for (int x = 0; x < 8; x++){
			for (int y = 0; y < 8; y++){
				white[x][y] = 0;
				black[x][y] = 0;
				returnMove[x][y] = 0;
			}
		}

		white[3][3] = 1;
		black[3][4] = 1;
		white[4][4] = 1;
		black[4][3] = 1;

		color = "wait";
		//displayThread = null;
	    id = 0;
	    locX = -1;
	    locY = -1;
	    text = null;
	    currentTurnText = "waiting";
		currentTurn = WAIT;
		isConnected = false;
		noMoreValidMovesBlack = false;
		noMoreValidMovesWhite = false;
		moveAgain = false;
		gameOver = false;
	}

	public void paint(Graphics chamber){
   		super.paint(chamber);

   		origX = (getWidth()/2)-width/2;
		origY = (getHeight()/2)-height/2;

   		chamber.setColor(Color.GRAY);
   		chamber.fillRect(origX,origY,width,height);

		chamber.setColor(Color.BLACK);

   		sizeX = width/8;
   		sizeY = height/8;

   		for(int i=0;i<9;i++){
   			for(int j=0;j<9;j++){
   				chamber.drawLine(i*sizeX+origX,j*sizeY+origY,width,j*sizeY+origY);
   				chamber.drawLine(i*sizeX+origX,j*sizeY+origY,i*sizeX+origX,height);
   			}
   		}

   		try{
   			printBoard(chamber);
   		}catch(Exception e){}

   		try{
			Thread.sleep(10);
        }catch(InterruptedException ie){}

		repaint();
	}

	public void run(){
		while(true){

			/*edit display text to user*/
			result.setText(userGram);
			myColor.setText(myColorIs);

			try{
				Thread.sleep(100);
			}
			catch(InterruptedException ie){
				System.out.println(ie);
			}
		}
    }

    private void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("Net Reversi Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Container cp = frame.getContentPane();
		cp.setLayout(new BorderLayout());

		this.setLayout(new FlowLayout());
		cp.add(this, BorderLayout.CENTER);

		JPanel resultsSuper = new JPanel();
		resultsSuper.setLayout(new GridLayout(2,1));

		JPanel threads = new JPanel();
		threads.setLayout(new FlowLayout());

		JPanel control = new JPanel();
		control.setLayout(new FlowLayout());
		control.add(connect);
		control.add(reset);

		JPanel results = new JPanel();
		results.setLayout(new GridLayout(2,1));

		results.add(result);
		results.add(myColor);

		control.add(results);

		resultsSuper.add(control);
		resultsSuper.add(threads);

		cp.add(resultsSuper, BorderLayout.SOUTH);

		connect.addActionListener(this);
		reset.addActionListener(this);

		//Display the window.
		frame.setSize(800,700);
		frame.setVisible(true);

		addMouseListener(this);

		this.addComponentListener(new ComponentListener(){
		    public void componentResized(ComponentEvent evt) {
		    	//System.out.println("resized!");
		    }
		    public void componentHidden(ComponentEvent evt){
		    }
		    public void componentShown(ComponentEvent evt){
		    }
		    public void componentMoved(ComponentEvent evt){
		    }
		});
    }

    public void updateCurrentTurn(Move move){
    	if(currentTurnText.equals("waiting") && !color.equals("observer")){
    		currentTurnText = move.getColor();
    		if(currentTurnText.equals("black")){
    			currentTurn = BLACK;
    		}
    		else if(currentTurnText.equals("white")){
    			currentTurn = WHITE;
    		}
    		userGram = "an opponent has joined! it is now your turn...";
    	}
    	else if(!color.equals("observer")){
    		currentTurnText = move.getColor();
    		if(currentTurnText.equals("black")){
    			currentTurn = BLACK;
    			//white[move.getLocationX()][move.getLocationY()] = 1;
    			int returnValue = makeMove(move.getLocationX(),move.getLocationY(),WHITE);

				System.out.println();
				System.out.println("white moved: "+returnValue);
				System.out.println();

				userGram = "opponent has moved, it is now your turn...";
    		}
    		else if(currentTurnText.equals("white")){
    			currentTurn = WHITE;
    			//black[move.getLocationX()][move.getLocationY()] = 1;
    			int returnValue = makeMove(move.getLocationX(),move.getLocationY(),BLACK);

				System.out.println();
				System.out.println("black moved: "+returnValue);
				System.out.println();

				userGram = "opponent has moved, it is now your turn...";
    		}
    		else if(move.getColor().equals("blackagain")){
    			int returnValue = makeMove(move.getLocationX(),move.getLocationY(),BLACK);
    			userGram = "you have no valid moves, waiting for opponent to move again...";
    		}
    		else if(move.getColor().equals("whiteagain")){
    			int returnValue = makeMove(move.getLocationX(),move.getLocationY(),WHITE);
    			userGram = "you have no valid moves, waiting for opponent to move again...";
    		}
    		else if(move.getColor().equals("gameover")){
    			if(color.equals("black")){
    				int returnValue = makeMove(move.getLocationX(),move.getLocationY(),WHITE);
    				String winner = printWinner();
    				userGram = "game over! "+winner+" has won.";
    			}
    			else if(color.equals("white")){
    				int returnValue = makeMove(move.getLocationX(),move.getLocationY(),BLACK);
    				String winner = printWinner();
    				userGram = "game over! "+winner+" has won.";
    			}
    		}
    		else if(move.getColor().equals("wait")){
    			userGram = "wait! please connect again to play.";
					if(isConnected){
						reset();
					}
    		}
    	}
    }

    NetReversiClient(){
    	super();

    	try{
    		url = new URL("http://localhost:8080/NetReversi/Reversi");
    		//url = new URL("http://tomcat-cspp.cs.uchicago.edu:8180/jdiomede/servlet/Reversi");
    	}catch(MalformedURLException mue){}

		factory = DocumentBuilderFactory.newInstance();
	    try{
	    	builder = factory.newDocumentBuilder();
	    }catch(ParserConfigurationException pce){}
		document = builder.newDocument();

		sfactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFile = new StreamSource(new File("Move.xsd"));
		try{
            schema = sfactory.newSchema(schemaFile);
        }catch(Exception e){}
        validator = schema.newValidator();

		Element root = document.createElement("move");
		document.appendChild(root);

		root.setAttribute("id",Integer.toString(id));

		Element locElX   = document.createElement("locationX");
		Element locElY   = document.createElement("locationY");
		Element colorEl = document.createElement("color");
		root.appendChild(locElX);
		root.appendChild(locElY);
		root.appendChild(colorEl);

		text = document.createTextNode(Integer.toString(locX));
		locElX.appendChild(text);

		text = document.createTextNode(Integer.toString(locY));
		locElY.appendChild(text);

		text = document.createTextNode(color);
		colorEl.appendChild(text);

		tFactory = TransformerFactory.newInstance();
		try{
			transformer = tFactory.newTransformer();
		}catch(TransformerConfigurationException tce){}

    	//initialize board
		for (int x = 0; x < 8; x++){
			for (int y = 0; y < 8; y++){
				//white[x][y] = 1;
				white[x][y] = 0;
				black[x][y] = 0;
			}
		}

		white[3][3] = 1;
		black[3][4] = 1;

		white[4][4] = 1;
		black[4][3] = 1;
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    new NetReversiClient().createAndShowGUI();
			}
		});
    }
}

class PollThread extends Thread {
	public boolean waiting = true;
	private NetReversiClient nrc = null;
	private URL url = null;
	private int id = 0;
	private String myColor = null;
	private String request = null;
	private static DocumentBuilderFactory factory = null;
	private static DocumentBuilder builder = null;
	private static Document document = null;
	private static TransformerFactory tFactory = null;
	private static Transformer transformer = null;
	private static Validator validator = null;
	private static SchemaFactory sfactory = null;
	private static Schema schema = null;
	private static Source schemaFile = null;

	static final int MESSAGE = -1;

	PollThread(NetReversiClient nrc, URL url, int id, String myColor){
		this.nrc = nrc;
		this.url = url;
		this.id = id;
		this.myColor = myColor;

		factory = DocumentBuilderFactory.newInstance();
	    try{
	    	builder = factory.newDocumentBuilder();
	    }catch(ParserConfigurationException pce){}
		document = builder.newDocument();

		sfactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFile = new StreamSource(new File("Move.xsd"));
		try{
            schema = sfactory.newSchema(schemaFile);
        }catch(Exception e){}
        validator = schema.newValidator();

		Element root = document.createElement("move");
		document.appendChild(root);

		root.setAttribute("id",Integer.toString(id));

		Element locElX   = document.createElement("locationX");
		Element locElY   = document.createElement("locationY");
		Element colorEl = document.createElement("color");
		root.appendChild(locElX);
		root.appendChild(locElY);
		root.appendChild(colorEl);

		Text text = null;

		text = document.createTextNode(Integer.toString(MESSAGE));
		locElX.appendChild(text);

		text = document.createTextNode(Integer.toString(MESSAGE));
		locElY.appendChild(text);

		text = document.createTextNode("request");
		colorEl.appendChild(text);

		tFactory = TransformerFactory.newInstance();
		try{
			transformer = tFactory.newTransformer();
		}catch(TransformerConfigurationException tce){}

		StringWriter sw = new StringWriter();
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(sw);
		try{
			transformer.transform(source, result);
		}catch(TransformerException tce){}

		request = sw.toString();
		request = request.replace(' ','+');
	}

	public void run(){
		while(waiting){
			HttpURLConnection conn = null;
			try{
				conn = (HttpURLConnection) url.openConnection();
			}catch(IOException ie){}
			try{
				conn.setRequestMethod("POST");
			}catch(ProtocolException pe){}
			conn.setRequestProperty("Content-Type", "text/xml");
			conn.setRequestProperty("Content-Language", "en-US");
			conn.setDoInput(true);
			conn.setDoOutput(true);

			PrintWriter pw = null;
			try{
				pw = new PrintWriter(conn.getOutputStream());
			}catch(IOException ie){}
			pw.write(request);
			pw.close();

			Scanner in = null;
			try{
				in = new Scanner(conn.getInputStream());
			}catch(IOException ie){}
			StringBuffer retVal = new StringBuffer();
			while (in.hasNext())
			    retVal.append(in.next());
			conn.disconnect();

			String response = retVal.toString();

			if(response != null){
				response = response.replace('+',' ');

				System.out.println("poll: "+response);

				try{
		            document = builder.parse(new org.xml.sax.InputSource(new StringReader(response)));
		        }catch(Exception e){}

		        try {
				    validator.validate(new DOMSource(document));
				} catch (SAXException se) {}catch (IOException ioe) {}

		        /*String color = null;
		        Element colorElement = (Element) document.getElementsByTagName("color").item(0);
		        color = colorElement.getFirstChild().getNodeValue();*/

		        int moveX;
		        int moveY;
				String color;

				Element locElementX = (Element) document.getElementsByTagName("locationX").item(0);
			    Element locElementY = (Element) document.getElementsByTagName("locationY").item(0);
				Element colorElement = (Element) document.getElementsByTagName("color").item(0);

				moveX = Integer.parseInt(locElementX.getFirstChild().getNodeValue());
			    moveY = Integer.parseInt(locElementY.getFirstChild().getNodeValue());
				color = colorElement.getFirstChild().getNodeValue();

				Move move = new Move(id,color,moveX,moveY);

				if(myColor.equals(color)){
					waiting = false;
					nrc.updateCurrentTurn(move);
				}
				else if( (myColor.equals("black") && color.equals("whiteagain")) ||
						 (myColor.equals("white") && color.equals("blackagain")) )
				{
					nrc.updateCurrentTurn(move);
				}
				else if(color.equals("gameover")){
					waiting = false;
					nrc.updateCurrentTurn(move);
				}
			}

			try{
				Thread.sleep(1000);
			}
			catch(InterruptedException ie){
				waiting = false;
			}
		}
	}
}

class Move{
    private int id;
    private String color;
    private int locationX;
    private int locationY;

    Move(int id, String color, int locX, int locY){
		this.id = id;
		this.color = color;
		this.locationX = locX;
        this.locationY = locY;
    }

    Move(Move copy){
		this(copy.getID(),copy.getColor(),copy.getLocationX(),copy.getLocationY());
    }

    public int getID(){
		return this.id;
    }

    public int getLocationX(){
		return this.locationX;
    }

    public int getLocationY(){
		return this.locationY;
    }

    public void setLocationX(int locationX){
		this.locationX = locationX;
    }

    public void setLocationY(int locationY){
		this.locationY = locationY;
    }

    public String getColor(){
		return this.color;
    }

    public void setColor(String color){
		this.color = color;
    }

}