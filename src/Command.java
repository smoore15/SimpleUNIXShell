import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Command {
		public static final Done finished = new Done();
		public String command;
		public String[] args;
		public Filter cmd;
		public LinkedBlockingQueue<Object> output;
		
		public Command(String command, String[] args){
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
			System.out.println("Something has gone very wrong.");
			return null;
		}
	private static class Done{
		private String boop;
		private Done(){
			this.boop = "This command is complete";
		}
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
				this.done = true;
				return finished;
			}
		}
	}
	public class ls extends Filter{
		
		public ls(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
		}

		@Override
		public Object transform(Object o) {
			File curr = new File(System.getProperty("user.dir"));
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
			this.done = true;
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
			this.done = true;
			return null;
		}
	}
	public class cat extends Filter{
		private File[] files;
		private Scanner scan;
		private int current;
		public cat(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
			files = new File[args.length];
			for(int i = 0;i<args.length;i++){
				files[i] = new File(args[i]);
			}
			current = 0;
			try {
				scan = new Scanner(files[current]);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		@Override
		public Object transform(Object o) {
			try {
				if(current==files.length-1 && !scan.hasNext()){
					this.done = true;
					return finished;
				}else{
					if(!scan.hasNext()){
						scan = new Scanner(files[++current]);
					}
					return scan.nextLine();
				}
			} catch (FileNotFoundException e) {
				System.out.println("File not found");
			}
			return null;
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
				this.done = true;
				return --count;
			}
			return null;
		}
	}
	public class history extends Filter{
		Iterator<String> iterator;
		public history(LinkedBlockingQueue<Object> in, LinkedBlockingQueue<Object> out) {
			super(in, out);
			iterator = MyShell.history.iterator();
		}

		@Override
		public Object transform(Object o) {			
			if(!iterator.hasNext()){
				this.done = true;
				return finished;
			}
			else{
				return iterator.next();
			}
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
		public Object transform(Object o) {	
			if(o!=null && o!=finished){
      			try {
					writer.write(o.toString() + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
			else if(o==finished){
				this.done = true;
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return finished;
			}
			return null;
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
				this.done = true;
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
				this.done = true;
			}
			return null;
		}
		
	}
}
