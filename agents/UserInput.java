package agents;

import java.util.Scanner;

public class UserInput {
	private static Scanner scanner = new Scanner(System.in);
	
	public static int promptIntUntilCorrect(String prompt, int min, int max){
		while(true){
			System.out.print(prompt + " " );
			int input = scanner.nextInt();
			if(input >= min && input <= max){
				return input;
			}
			System.out.println("Invalid input!");
		}
	}
	
}
