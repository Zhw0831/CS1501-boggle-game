import java.io.*;
import java.util.*;

public class Crossword{
	private DictInterface D;
	private char[][] theBoard;
	private StringBuilder[] colBuilder;
	private StringBuilder[] rowBuilder;
	private HashMap<String, Integer> scoreMap;
	private char[] alphabet = {'a','b','c','d','e','f','g',
							   'h','i','j','k','l','m','n',
							   'o','p','q','r','s','t','u',
							   'v','w','x','y','z'};
	private int size;
	ArrayList<Integer>[] lastIndex;
	private long start;

	public static void main(String[] args) throws Exception{
		if(args.length < 3){
			System.out.print("Please enter three file names: <dictionary> <board> <letterpoints>");
			System.exit(0);
		}

		String dictionary = args[0];
		String boardFile = args[1];
		String letterPoints = args[2];
		new Crossword(dictionary, boardFile, letterPoints);
		System.out.println("Solution not found");
	}

	public Crossword(String dictionary, String boardFile, String letterPoints) throws Exception{
		String dic = dictionary;
		String board_file = boardFile;
		String letter_points = letterPoints;
		readIn(dic, board_file);
		points(letter_points);

		// set up the rowBuilder, and colBuilder based on the read-in size
		rowBuilder  = new StringBuilder[size];
		colBuilder = new StringBuilder[size];

		// set up the stringBuilder for each row and col
		for(int i = 0; i<size; i++){
			rowBuilder[i] = new StringBuilder();
			colBuilder[i] = new StringBuilder(); 
		}

		start = System.nanoTime();

		solve(0,0);
	}

	@SuppressWarnings("unchecked")
	private void readIn(String file1, String file2) throws Exception{
		//read in the dictionary file
		Scanner reader1 = new Scanner(new File(file1));
		String word;
		D = new MyDictionary();

		while (reader1.hasNext()){
			word = reader1.nextLine();
			D.add(word);
		}
		reader1.close();

		//read in the board
		BufferedReader fileReader = new BufferedReader(new FileReader(file2));		

		while(fileReader.ready()){
			size = Integer.parseInt(fileReader.readLine());

		theBoard = new char[size][size];
		lastIndex = new ArrayList[size];

		 	for(int row = 0; row<size; row++){
		 		String words = fileReader.readLine();
				for(int col = 0; col<size; col++){
					theBoard[row][col] = words.charAt(col);
				}
			}
		 }
		fileReader.close();

		for(int i=0;i<size;i++){
			lastIndex[i] = new ArrayList<Integer>();
		}
		for(int i=0;i<size;i++){
			for(int j=0;j<size;j++){
				if(theBoard[i][j]=='-'){
					lastIndex[i].add(j);
				}
			}
		}
	}

	private void points(String file) throws Exception{
		Scanner scoreReader = new Scanner(new File(file));
		//read in the scores for each character and store in a hashmap for look up later
		scoreMap = new HashMap<String,Integer> ();
		while(scoreReader.hasNext()){
			String letter = scoreReader.next();
			int point = scoreReader.nextInt();
			scoreMap.put(letter, point);
		}
		scoreReader.close();
	}

	private void solve(int i, int j){
		char s = theBoard[i][j];
		switch(s) {
			// first case, the slot is open
			case '+':
				for (int r = 0; r < alphabet.length; r++) {
					char c = alphabet[r];
					if (isValid(i, j, c)) {
						rowBuilder[i].append(c);
						colBuilder[j].append(c);
						// break, when the board is finished filling in
						if (i == size - 1 && j == size - 1) {
							// done. Print the matrix out
							print_Sol();
							System.out.println("");
							// print the score out
							printScore();
							long finish = System.nanoTime();
							long delta = finish - start;
							System.out.println("Time running the algorithm: " + delta / 1000000000.0 + "s");
							System.exit(0);
						} else {
							// not full, keep searching
							if (j == size - 1 && i < size - 1)
								solve(i + 1, 0);
							else
								solve(i, j + 1);
							// backtracking
							delete(i, j);
						}
					}
				}
				break;

			// second case, if the cell is solid and cannot fill in
			case '-':
				// the last index of '-' in a rowBuilder
				int last_index;
				// the largest index of '-' before this '-' in a colBuilder
				int largestIndexRow;
				ArrayList<Integer> index_arr = lastIndex[i];

				// get the ArrayList index of the col j
				int current = index_arr.indexOf(j);
				// current !=0 means there are more than one '-' in the row and we are not on the first one
				if (current != 0) {
					int before = current - 1;
					last_index = index_arr.get(before);
				} else
					last_index = -1;

				// check the left of the last index (should be a valid word)
				StringBuilder new_left = new StringBuilder();
				String str1 = rowBuilder[i].substring(last_index + 1, rowBuilder[i].length());
				int check1;
				// if two '-' are neighbors in a row, it is ok
				if(last_index + 1 == j)
					check1 = 2;
				else
					check1 = D.searchPrefix(new_left.append(str1));

				int check2;
				if(colBuilder[j].toString().contains("-")){
					largestIndexRow = colBuilder[j].lastIndexOf("-");
					StringBuilder new_up = new StringBuilder();
					// get the substring between the last '-' and this '-'
					String str2 = colBuilder[j].substring(largestIndexRow+1,colBuilder[j].length());
					// if two '-' are neighbors in a col, it is also ok
					if(largestIndexRow + 1 ==i)
						check2 = 2;
					else check2 = D.searchPrefix(new_up.append(str2));
				}
				else{
					// the builder doesn't contain '-', simply check the whole existing string
					check2 = D.searchPrefix(colBuilder[j]);
				}

				// is valid when rowBuilder gives a valid word and colBuilder gives a valid word;
				// 			when we are on the (0,0) of the board (both string builders are empty);
				//			when we have a valid word in the row but colBuilder is empty;
				// 			when we have a valid word in the col but rowBuilder is empty.
				if ((check1 >= 2 && (check2 >= 2 || i == 0)) || ((check1 >= 2 || j == 0) && check2 >= 2) || (i == 0 && j == 0)) {
					rowBuilder[i].append('-');
					colBuilder[j].append('-');
					if (i == size - 1 && j == size - 1) {
						// done. Print the matrix out
						print_Sol();
						System.out.println("");
						// print the score out
						printScore();
						long finish = System.nanoTime();
						long delta = finish - start;
						System.out.println("Time running the algorithm: " + delta / 1000000000.0 + "s");
						System.exit(0);
					} else {
						if (j == size - 1 && i < size - 1)
							solve(i + 1, 0);
						else
							solve(i, j + 1);
						delete(i, j);
					}
				}
				break;

			// default case, if it's the pre-set letter
			default:
				rowBuilder[i].append(theBoard[i][j]);
				colBuilder[j].append(theBoard[i][j]);
				if (i == size - 1 && j == size - 1) {
					// done. Print the matrix out
					print_Sol();
					System.out.println("");
					// print the score out
					printScore();
					long finish = System.nanoTime();
					long delta = finish - start;
					System.out.println("Time running the algorithm: " + delta / 1000000000.0 + "s");
					System.exit(0);
				}
				else{if (j == size - 1 && i < size - 1)
					solve(i + 1, 0);
				else
					solve(i, j + 1);
				delete(i, j);
		}
				break;
		}

	}

	private boolean isValid(int i, int j, char c){
		StringBuilder new1 = new StringBuilder();
		if(rowBuilder[i].toString().contains("-"))
			// we just check the substring before the '-' sign. It can be substring between two '-' signs
			new1 = new1.append(rowBuilder[i].substring(rowBuilder[i].lastIndexOf("-")+1,rowBuilder[i].length()));
		else
			new1 = new1.append(rowBuilder[i].toString());

		StringBuilder new2 = new StringBuilder();
		if(colBuilder[j].toString().contains("-"))
			// same approach
			new2 = new2.append(colBuilder[j].substring(colBuilder[j].lastIndexOf("-")+1,colBuilder[j].length()));
		else
			new2 = new2.append(colBuilder[j].toString());

		int check1 = D.searchPrefix(new1.append(c));
		int check2 = D.searchPrefix(new2.append(c));

		// set by the rules:
		// - If `j` is not an end index, then `rowStr[i]` + the letter a must be a valid prefix in the dictionary
		// - If `j` is an end index, then `rowStr[i]` + the letter must be a valid word in the dictionary
		// - If `i` is not an end index, then `colStr[j]` + the letter must be a valid prefix in the dictionary
		// - If `i` is an end index, then `colStr[j]` + the letter must be a valid word in the dictionary
		if(j!=size-1){
      		if(check1==0){
      			return false;
      		}
      	}
      	if(j==size-1){
      		if(check1 < 2){
      			return false;
      		}
      	}

      	if(i!=size-1){
      		if(check2==0){
      			return false;
      		}
      	}
      	if(i==size-1){
      		if(check2 < 2){
      			return false;
      		}
      	}

      	if(j==size-1 && i==size-1){
      		if(check1<2 || check2<2){
      			return false;
      		}
      	}
      	return true;
	}

	private void delete(int row, int col){
		// we delete the char we just append
		rowBuilder[row].deleteCharAt(rowBuilder[row].length()-1);
		colBuilder[col].deleteCharAt(colBuilder[col].length()-1);
	}

	private void print_Sol(){
		System.out.println("Solution found:");

		// write the solution back to the board
		for(int i=0;i<size;i++){
			String str = rowBuilder[i].toString();
			for(int j=0;j<size;j++){
				if(theBoard[i][j]!='-'){
					theBoard[i][j] = str.charAt(j);
				}
			}
		}

		for(int i=0;i<size;i++){
			for(int j=0;j<size;j++){
				System.out.print(theBoard[i][j]);
			}
			System.out.println("");
		}
	}

	private void printScore(){
		// look up the letter-score look up hashmap to calculate the total score
		int score = 0;
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				char c = theBoard[i][j];
				if(c!='-')
					score += scoreMap.get(Character.toString(Character.toUpperCase(c)));
			}
		}
		System.out.println("Score: " + score);
	}
}