import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Command {
		public static final String finished = "(Q*&#$(*Q#)&%Q(*#$&Q)(_EHCALSJC()EW00000000";
		public String command;
		public String[] args;
		public Filter cmd;
		public LinkedBlockingQueue<Object> output;
		public boolean produces;
		public boolean consumes;
		
		public Command(String command, String[] args){
			if(command.equals(">")){
				command = "redirect";
			}
			this.args = args;
			this.command = command;
		}
		
		public Filter getCommands(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out){
			Class<?>[] tmp = Command.class.getClasses();
			for(Class<?> c : tmp){
				if(c.getName().equals("Command$"+command)){
					try {
						Constructor con = c.getConstructor(Command.class,LinkedBlockingQueue.class,LinkedBlockingQueue.class);
						Filter instance = (Filter) con.newInstance(this,in,out);
						output = out;
						return instance;
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println(command + ": invalid command");
			return null;
		}
	public class pwd extends Filter{
		public boolean returned = false;
		public pwd(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}
		@Override
		public Object transform(Object o) {
			if(!returned){
				returned = true; 
				return System.getProperty("user.dir");
			}else{
				done = true;
				return finished;
			}
		}
	}
	public class ls extends Filter{
		//TODO: make this use the super.run()
		@Override
		public void run(){
			out = (LinkedBlockingQueue<Object>) transform(out);
		}
		public ls(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}

		@Override
		public Object transform(Object o) {
			File curr = new File(System.getProperty("user.dir"));
			String r = "";
			for(String s : curr.list()){
				try {
					out.put(s);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			try {
				out.put(finished);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return out;
		}
	}
	public class cd extends Filter{

		public cd(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}
		@Override
		public Object transform(Object o) {
			if(args[0].equals("..")){
				System.setProperty("user.dir", new File(System.getProperty("user.dir")).getParent());
			}
			else if(args[0].equals(".")){
			}			
			else{
				File f = new File(System.getProperty("user.dir"));
				System.setProperty("user.dir", new File(System.getProperty("user.dir") + f.separator + args[0]).getAbsolutePath());
			}
			done = true;
			return null;
		}
	}
	public class cat extends Filter{
		
		public cat(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}
		
		@Override
		public void run(){
			out = (LinkedBlockingQueue<Object>) transform(out);
		}

		@Override
		public Object transform(Object o) {
			try {
				for(String s : args){
					Scanner scan = new Scanner(new File(s));
					while (scan.hasNextLine()){
						out.put(scan.nextLine());
					}
				}
				out.put(finished);//TODO: decide if to use instance of DoneObject
			} catch (FileNotFoundException e) {
				System.out.println("File not found");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return out;
		}
	}
	public class lc extends Filter{
		private Integer count = 0;
		public lc(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}

		@Override
		public Object transform(Object o) {
			count++;
			if(o==finished){
				done = true;
				return --count;
			}
			return null;
		}
	}
	public class history extends Filter{

		public history(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}
		@Override
		public void run(){
			transform(MyShell.history);
		}

		@Override
		public Object transform(Object o) {
			LinkedList<String> list = (LinkedList<String>) o;
			
			for(String s : list){
				try {
					out.put(s);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			try {
				out.put(finished);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
	public class sleep extends Filter{
		long duration = Integer.parseInt(args[0]);
		public sleep(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}
		@Override
		public Object transform(Object o) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println(Thread.currentThread().getName() + " 's sleep has been interrupted.");
			}
			
			if (duration == 0) {
				this.done = true;
				return finished;
			}
			return "Sleep: " + --duration + " seconds left.";
		}
	}
	public class redirect extends Filter{
		public File file;
		public Writer writer;
		
		public redirect(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
			file = new File(args[0]);
			try {
				writer = new BufferedWriter(new FileWriter(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void run() {
	        Object o = null;
	        while(o!=finished) {
				// read from input queue, may block
	            try {
	            	if(in!=null){
	            		o = in.take();
	            	}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}    
	            o = transform(o);
	            try {
	            	if(o!=null && o!=finished){
	          			writer.write(o.toString() + "\n");
	            	}
				} catch (IOException e) {
					e.printStackTrace();
				}       
	        }
	        try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public Object transform(Object o) {	
			if(o==finished){
				done=true;
				return finished;
			}
			return o;
		}
	}
	public class grep extends Filter{

		public grep(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}

		@Override
		public Object transform(Object o) {
			//take in a line, if it contains arg[0], return.
			if(o==finished){
				done = true;
				return o;
			}
			if(((String)o).contains(args[0])){
				return o;
			}else{
				return null;
			}
		}
		
	}
	public class shellsink extends Filter{

		public shellsink(BlockingQueue<Object> in, BlockingQueue<Object> out) {
			super(in, out);
		}

		@Override
		public Object transform(Object o) {
			if(o!=finished){
				System.out.println(o);
			}
			if(o==finished){
				done = true;
			}
			return null;
		}
		
	}
}
