import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * This class manages the lifetime of commands. You need to modify this class such that it can:
 *     1. validate the command
 *     2. create subcommands
 *     3. execute subcommands
 *     4. suspend/stop/resume the command if you are doing Part 4.
 * 
 */
public class CommandManager {
	public String command;
	public Command[] arr;
	public Thread[] tArr;
	public CommandManager(String command) {
		//Create the message buffer.

		if(command.equals("exit")){	return;	}
		//Create the producer and consumer threads and pass each thread
		//a reference to the mailbox object.
		
		arr = parse(command);
		if(arr==null){ return; }
		
		tArr = new Thread[arr.length];
		
		//use the Command.subC.in or out
		for(int i = 0; i < arr.length;i++){
		try {
			LinkedBlockingQueue<Object> mbox = null;
			if(i!=0){
				mbox  = arr[i-1].output;
			}
				tArr[i] = arr[i].getCommands(mbox, new LinkedBlockingQueue<Object>());
				tArr[i].start();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		if(producesOutput(arr[arr.length-1].command)){
			Thread boop = new Command(command, null).new shellsink(arr[arr.length-1].output,null);
			boop.start();

			for(Thread t : tArr){
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			try {
				boop.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}else{
			for(Thread t : tArr){
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	private boolean producesOutput(String cmd) {
		switch (cmd){
		case "sleep": return true;
		case "redirect": return false;
		case "cat": return true;
		case "grep": return true;
		case "pwd": return true;
		case "ls": return true;
		case "cd": return false;
		case "lc": return true;
		case "history": return true;
		case "exit": return false;
		default: return false;
		}
	}
	/**
	 * This is for Part 4
	 */
	public void kill() {
		
		
	}
	private Command[] parse(String command){
		if(command.charAt(0)=='>'){
			System.out.println("invalid pipe order");
			return null;
		}
		if(command.contains(">")){
			command = command.replace(">", "| redirect");
		}
		String[] splitPipe = command.split("\\|");//cat arg1.txt arg2.txt | grep test > stuff.txt
												   //[cat, arg1.txt, arg2.txt][grep, test][redirect, stuff.txt]
		Command[] commandsArr = new Command[splitPipe.length];
		int j = 0;
		for(String s : splitPipe){
			s = s.trim();
			String[] tmp = s.split("\\s+");
			if(tmp.length==1){
				if(validate(tmp[0], null)){
					commandsArr[j++] = new Command(tmp[0], null);
				}else{
					return null;
					}
			}else if(tmp.length==2){
					if(validate(tmp[0],new String[]{tmp[1]})){
						commandsArr[j++] = new Command(tmp[0], new String[]{tmp[1]});
					}else{
						return null;
						}
				}else if(validate(tmp[0], Arrays.copyOfRange(tmp,1,tmp.length))){
					commandsArr[j++] = new Command(tmp[0], Arrays.copyOfRange(tmp,1,tmp.length));
				}else{
					return null;
					}
		}
		if(validatePipingOrder(commandsArr)){
			return commandsArr;
		}
		return null;
	}

	private boolean validate(String command, String[] args){
		if(command.equals(">")){
			command = "redirect";
		}
		if(validateCommand(command)){
				switch (command){
				case "sleep":	return validateSleep(args);
				case "redirect": return validateRedirect(args);
				case "cat": 	return validateCat(args);
				case "grep":	return validateGrep(args);
				case "cd":		return validateCd(args);
				default:	if(args==null){return true;}
							else{return false;}
				}
			}
		else{
			System.out.println(command +": invalid command");
			return false;
		}
	}
	
	private boolean validateCommand(String cmd){
		switch (cmd){
		case "sleep": return true;
		case "redirect": return true;
		case "cat": return true;
		case "grep": return true;
		case "pwd": return true;
		case "ls": return true;
		case "cd": return true;
		case "lc": return true;
		case "history": return true;
		case "exit": return true;
		default: return false;
		}
	}
	private boolean validateSleep(String[] args){
		try {	if(args.length==1){
					int data = Integer.parseInt(args[0]);
					if (data>=0){
						return true;
					}else{
						System.out.println("sleep: invalid argument");
						return false;
					}
				}else{
					System.out.println("sleep: invalid argument");
					return false;
				}
		} 
		catch(NumberFormatException e) { 
			System.out.println("sleep: invalid argument");
			return false;
		}
		catch(NullPointerException e){
			System.out.println("sleep: missing argument");
			return false;
		}
	}
	private boolean validateRedirect(String[] args){
		try {	if(args.length == 1){
				File f = new File(args[0]);
					if(f.createNewFile() || f.canWrite()){
						return true;
					}else{
						System.out.println(">: invalid argument");
						return false;
					}
			}else{
				System.out.println(">: invalid argument");
				return false;
			}
		} 
		catch(NullPointerException e){
			System.out.println(">: missing argument");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	private boolean validateCat(String[] args){
		boolean argsV = false;
		try{
			for(String f : args){
				File file = new File(f);
				if(file.isFile() && file.canRead()){
					argsV = true;
				}else{
					System.out.println("cat: file not found");
					return false;
				}
			}
		}catch(NullPointerException e){
			System.out.println("cat: missing argument");
			return false;
		}
		return argsV;
	}
	private boolean validateGrep(String[] args){
		try{	if(args.length==1){
					if(args[0] instanceof String){
							if(!args[0].contains(" ")){
								return true;
							}
						}
				}else{
					System.out.println("grep: invalid argument");
					return false; 
				}
			}catch(NullPointerException e){
				System.out.println("grep: missing argument");
				return false;
			}
		return false;
	}
	private boolean validateCd(String[] args){
		try{	if(args.length==1){
					if(args[0].equals(".")||args[0].equals("..")){
							return true;
						}else if (new File(System.getProperty("user.dir") + new File(args[0]).separator + args[0]).isDirectory()){
							return true;
						}else{
							System.out.println("cd: directory not found");
							return false;
						}
				}
			}catch(NullPointerException e){
				System.out.println("cd: missing argument");
				return false;
			}
		return false;
	}
	private boolean takesNoInput(Command cmd){
		switch (cmd.command){
		case "ls": return true;
		case "pwd": return true;
		case "cd": return true;
		case "sleep": return true;
		case "cat": return true;
		case "history": return true;
		default: return false;
		}
	}
	private boolean validatePipingOrder(Command[] cmds){
		if(cmds.length==1){
			if(!takesNoInput(cmds[0])){
				System.out.println("invalid pipe order");
				return false;
			}
		}else{
			if(cmds[0].command.equals("exit")){
				System.out.println("invalid pipe order");
				return false;
			}
			if(cmds[0].command.equals("cd")){
				System.out.println("invalid pipe order");
				return false;
			}
			if(takesNoInput(cmds[0])){
				for(int i = 1;i<cmds.length;i++){
					if(takesNoInput(cmds[i])){
						System.out.println("invalid pipe order");
						return false;
					}
				}
			}else{
				System.out.println("invalid pipe order");
				return false;
			}
		}
		return true;
	}
}
