import java.util.LinkedList;
import java.util.Scanner;

public class MyShell {
	public static LinkedList<String> history;
	//Your REPL resides here
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		String cmd = "";
		history = new LinkedList<String>();
		
		while (!cmd.equals("exit")){
			System.out.print("> ");
			cmd = input.nextLine();
			if (!cmd.contains("history")){
				history.add(cmd);
			}			
			CommandManager parent = new CommandManager(cmd);
		}
		System.out.println("REPL exits. Bye.");
	}

}
