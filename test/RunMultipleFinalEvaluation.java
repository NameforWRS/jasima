import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RunMultipleFinalEvaluation {

	public static void main(String[] args)
	{		
		File directory = new File("");//设定为当前文件夹 
		try{ 
		    System.out.println(directory.getCanonicalPath());//获取标准的路径 
		    System.out.println(directory.getAbsolutePath());//获取绝对路径 
		}catch(Exception e){}
		String batFile = args[0].split("-")[0];
		String beginIndex = args[0].split("-")[1];
		String count = args[0].split("-")[2];
		
		for(int i = Integer.parseInt(beginIndex); i < Integer.parseInt(count); i++)
		{
			String strcmd = "cmd /c start " + batFile + " " + i;  //调用我们在项目目录下准备好的bat文件，如果不是在项目目录下，则把“你的文件名.bat”改成文件所在路径。
			run_cmd(strcmd);  //调用上面的run_cmd方法执行操作
		}
		
	
	}
	
	public static void run_cmd(String strcmd) {
		//
		        Runtime rt = Runtime.getRuntime(); //Runtime.getRuntime()返回当前应用程序的Runtime对象
		        Process ps = null;  //Process可以控制该子进程的执行或获取该子进程的信息。
		        try {
		            ps = rt.exec(strcmd);   //该对象的exec()方法指示Java虚拟机创建一个子进程执行指定的可执行程序，并返回与该子进程对应的Process对象实例。
		            ps.waitFor();  //等待子进程完成再往下执行。
		        } catch (IOException e1) {
		            e1.printStackTrace();
		        } catch (InterruptedException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        }

		        int i = ps.exitValue();  //接收执行完毕的返回值
		        if (i == 0) {
		            System.out.println("执行完成.");
		        } else {
		            System.out.println("执行失败.");
		        }

		        ps.destroy();  //销毁子进程
		        ps = null;   
		    }
}
