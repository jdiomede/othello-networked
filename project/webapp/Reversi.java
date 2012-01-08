package Main;


import java.io.IOException;
import org.xml.sax.SAXException;

import java.io.PrintWriter;
import javax.servlet.ServletException;
import java.net.MalformedURLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.validation.*;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.net.URL;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import java.util.Random;

import javax.servlet.*;


@WebServlet(name = "Reversi", urlPatterns = {"/Reversi"})
public class Reversi extends HttpServlet {

    static final int WAIT = 0;
    static final int WHITE = 1;
    static final int BLACK = 2;
    static final int OBSERVER = 3;
    static final int BLACKAGAIN = 4;
    static final int WHITEAGAIN = 5;
    static final int GAMEOVER = 6;

    private static DocumentBuilderFactory factory = null;
    private static DocumentBuilder builder = null;
    private static Document document = null;
    private static SchemaFactory sfactory = null;
    private static Source schemaFile = null;
    private static Schema schema = null;
    private static Validator validator = null;
    private static TransformerFactory tFactory = null;
    private static Transformer transformer = null;

    private boolean gameOver;
    private int opponentWhite;
    private int opponentBlack;
    private int currentTurn;

    private int white[][] = new int[8][8];
    private int black[][] = new int[8][8];
    private int returnMove[][] = new int[8][8];
    private int validMoves[][][] = new int[8][8][8];

    private int pushX = -1;
    private int pushY = -1;

    private boolean noMoreValidMovesBlack = false;
    private boolean noMoreValidMovesWhite = false;

    public void init(){
        factory = DocumentBuilderFactory.newInstance();

        try{
            builder = factory.newDocumentBuilder();
        }catch(ParserConfigurationException pce){}

		sfactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		/*schemaFile = new StreamSource(new File("Move.xsd"));

        try{
            schema = sfactory.newSchema(schemaFile);
        }catch(Exception e){}

        validator = schema.newValidator();*/

		try{
        	tFactory = TransformerFactory.newInstance();
        }catch(TransformerFactoryConfigurationError tce){}

        try{
			transformer = tFactory.newTransformer();
        }catch(TransformerConfigurationException tce){}

        gameOver = false;
        opponentWhite = 0;
        opponentBlack = 0;
        currentTurn = WAIT;

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

    private static Move unMarshalInput(String xmlString) throws Exception{

        int id;
        int moveX;
        int moveY;
		String color;

        try{
            document = builder.parse(new org.xml.sax.InputSource(new StringReader(xmlString)));
        }catch(Exception e){}

		/*try {
		    validator.validate(new DOMSource(document));
		} catch (SAXException se) {
		    System.out.println(se);
		}
	        catch (IOException ioe) {
		    System.out.println(ioe);
		}*/

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

    private void findStreak(int x, int y, int direction, int color){
        int myMoves[][] = new int[8][8];
        int opMoves[][] = new int[8][8];
        switch(color){
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

    private void findValidMoves(int color){
        int myMoves[][] = new int[8][8];
        int opMoves[][] = new int[8][8];
        switch(color){
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
                                    findStreak(x-1,y-1,i,color);
                                }
                            break;
                            case 1:
                                //north
                                if(((y-1)>0) && opMoves[x][y-1] == 1){
                                    findStreak(x,y-1,i,color);
                                }
                            break;
                            case 2:
                                //northeast
                                if(((x+1)<8 && (y-1)>0) && opMoves[x+1][y-1] == 1){
                                    findStreak(x+1,y-1,i,color);
                                }
                            break;
                            case 3:
                                //west
                                if(((x-1)>0) && opMoves[x-1][y] == 1){
                                    findStreak(x-1,y,i,color);
                                }
                            break;
                            case 4:
                                //east
                                if(((x+1)<8) && opMoves[x+1][y] == 1){
                                    findStreak(x+1,y,i,color);
                                }
                            break;
                            case 5:
                                //southwest
                                if(((x-1)>0 && (y+1)<8) && opMoves[x-1][y+1] == 1){
                                    findStreak(x-1,y+1,i,color);
                                }
                            break;
                            case 6:
                                //south
                                if(((y+1)<8) && opMoves[x][y+1] == 1){
                                    findStreak(x,y+1,i,color);
                                }
                            break;
                            case 7:
                                //southeast
                                if(((x+1)<8 && (y+1)<8) && opMoves[x+1][y+1] == 1){
                                    findStreak(x+1,y+1,i,color);
                                }
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean checkMove(int movx, int movy, int color){
        boolean returnValue = false;

        int myMoves[][] = new int[8][8];
        int opMoves[][] = new int[8][8];
        switch(color){
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
        findValidMoves(color);

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
                switch(color){
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

    public int makeMove(int x, int y, int color){
        int localCurrentTurn = color;
        switch(currentTurn){
            case BLACKAGAIN:
                localCurrentTurn = BLACK;
            break;
            case WHITEAGAIN:
                localCurrentTurn = WHITE;
            break;
        }
        if(color == localCurrentTurn){
            if(checkMove(x,y,localCurrentTurn)){
                switch(localCurrentTurn){
                    case BLACK:
                        currentTurn = WHITE;

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
                                currentTurn = BLACKAGAIN;
                                //opponentWhite.pushMove(BLACK);
                            }
                        }
                        else{
                            //opponentWhite.pushMove(WHITE);
                        }
                    break;
                    case WHITE:
                        currentTurn = BLACK;

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
                                currentTurn = WHITEAGAIN;
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

    private Move serviceRequest(Move move){
        String currentTurnAsString = null;
        switch(currentTurn){
            case WAIT:
                currentTurnAsString = "wait";
            break;

            case BLACK:
                move.setLocationX(pushX);
                move.setLocationY(pushY);
                currentTurnAsString = "black";
            break;

            case BLACKAGAIN:
                move.setLocationX(pushX);
                move.setLocationY(pushY);
                currentTurnAsString = "blackagain";
            break;

            case WHITE:
                move.setLocationX(pushX);
                move.setLocationY(pushY);
                currentTurnAsString = "white";
            break;

            case WHITEAGAIN:
                move.setLocationX(pushX);
                move.setLocationY(pushY);
                currentTurnAsString = "whiteagain";
            break;

            case GAMEOVER:
                move.setLocationX(pushX);
                move.setLocationY(pushY);
                currentTurnAsString = "gameover";
            break;

            default:
            break;
        }
        move.setColor(currentTurnAsString);
        return move;
    }

    private Move registerClient(Move move) throws Exception{
        if(gameOver){
            move = reset(move);
    	}
    	switch(currentTurn){
            case WAIT:
                if(opponentBlack == 0){
                    //opponentBlack = client;
                    Random randomGenerator = new Random();
                    opponentBlack = randomGenerator.nextInt(99)+1;
                    Element root = document.getDocumentElement();
                    root.setAttribute("id",Integer.toString(opponentBlack));
                    currentTurn = WAIT;

                    move.setColor("black");
                }
                else if(opponentWhite == 0){
                    //opponentWhite = client;
                    Random randomGenerator = new Random();
                    do{
                        opponentWhite = randomGenerator.nextInt(99)+1;
                    }while(opponentWhite == 0 || opponentWhite == opponentBlack);

                    Element root = document.getDocumentElement();
                    root.setAttribute("id",Integer.toString(opponentWhite));
                    currentTurn = BLACK;

                    //call client interfaces for game start
                    //opponentBlack.startGame(currentTurn);
                    //opponentWhite.startGame(currentTurn);

                    move.setColor("white");
                }
            break;

            case BLACK:
            case WHITE:
            case WHITEAGAIN:
            case BLACKAGAIN:
                //do not allow more than two peers to register
                move.setColor("observer");
                //observersList.add(client);
            break;
    	}

        return move;
    }

    private Move reset(Move move) throws Exception{
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

        pushX = -1;
        pushY = -1;

        currentTurn = WAIT;
        noMoreValidMovesBlack = false;
        noMoreValidMovesWhite = false;
        gameOver = false;

        opponentWhite = 0;
        opponentBlack = 0;

        move.setColor("wait");

        return move;
    }

    private Move processInput(Move move) throws Exception{
        if(move.getColor().equals("wait")){
            move = registerClient(move);
        }
        else if(move.getColor().equals("reset")){
            move = reset(move);
        }
        else if( (move.getColor().equals("black") && currentTurn == BLACK) ||
                 (move.getColor().equals("black") && currentTurn == BLACKAGAIN) ||
                 (move.getColor().equals("white") && currentTurn == WHITE) ||
                 (move.getColor().equals("white") && currentTurn == WHITEAGAIN) ){

            int localCurrentTurn = currentTurn;
            switch(currentTurn){
                case BLACKAGAIN:
                    localCurrentTurn = BLACK;
                break;
                case WHITEAGAIN:
                    localCurrentTurn = WHITE;
                break;
            }

            int returnValue = makeMove(move.getLocationX(),move.getLocationY(),localCurrentTurn);

            switch(returnValue){
                case BLACK:
                    pushX = move.getLocationX();
                    pushY = move.getLocationY();
                    move.setColor("black");
                break;

                case BLACKAGAIN:
                    pushX = move.getLocationX();
                    pushY = move.getLocationY();
                    move.setColor("blackagain");

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

                    if(noMoreValidMovesBlack){
                        //game over
                        gameOver = true;
                        currentTurn = GAMEOVER;
                        move.setColor("gameover");
                        //opponentWhite.pushMove(GAMEOVER);
                    }
                break;

                case WHITE:
                    pushX = move.getLocationX();
                    pushY = move.getLocationY();
                    move.setColor("white");
                break;

                case WHITEAGAIN:
                    pushX = move.getLocationX();
                    pushY = move.getLocationY();
                    move.setColor("whiteagain");

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

                    if(noMoreValidMovesWhite){
                        //game over
                        gameOver = true;
                        currentTurn = GAMEOVER;
                        move.setColor("gameover");
                        //opponentWhite.pushMove(GAMEOVER);
                    }
                break;

                case GAMEOVER:
                    pushX = move.getLocationX();
                    pushY = move.getLocationY();
                    move.setColor("gameover");
                break;
            }
        }
        else if(move.getColor().equals("request")){
            move = serviceRequest(move);
        }
        else if(currentTurn == GAMEOVER){
            move.setColor("gameover");
        }
        return move;
    }

    private String processOutput(Move move) throws Exception{
        if(move != null){
            Element locElementX = (Element) document.getElementsByTagName("locationX").item(0);
            Element locElementY = (Element) document.getElementsByTagName("locationY").item(0);
            Element colorElement = (Element) document.getElementsByTagName("color").item(0);

            locElementX.getFirstChild().setNodeValue(Integer.toString(move.getLocationX()));
            locElementY.getFirstChild().setNodeValue(Integer.toString(move.getLocationY()));
            colorElement.getFirstChild().setNodeValue(move.getColor());
        }

        StringWriter sw = new StringWriter();
        String outputAsString = null;
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(sw);
        transformer.transform(source, result);

        //System.out.println(sw.toString());
        return sw.toString();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //we're going to pull xml off of http request payload
	Scanner in = new Scanner(request.getInputStream());
	StringBuffer input = new StringBuffer();
	//read payload line by line into StringBuffer
	while (in.hasNext()){
	    input.append(in.next());
	}

	String inputAsString = input.toString();
        inputAsString = inputAsString.replace('+',' ');

        String outputAsString = null;

        Move move = null;
	try{
	    move = unMarshalInput(inputAsString);
	}catch(Exception e){move = null;}

        try{
	    move = processInput(move);
	}catch(Exception e){move = null;}

        try{
	    outputAsString = processOutput(move);
	}catch(Exception e){}

        outputAsString = outputAsString.replace(' ','+');

	//echo what was received
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
	out.println(outputAsString);
	out.close();
    }

    public String getServletInfo() {
        return "Don't ask.";
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
