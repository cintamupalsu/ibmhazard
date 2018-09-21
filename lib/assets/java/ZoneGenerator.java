package bin;
import java.io.*;
//0. file ASC
//1. file seed result
//2. region id
//3. file txt result
class ZoneGenerator{
	public static void main(String args[]) {
		AscHeader fh = new AscHeader(args[0]);
		ZoneWriter zw = new ZoneWriter(fh, args[1], args[2]);
		WriterTxt wt = new WriterTxt(fh, args[3], args[2]);
		zw.t.start();
		wt.t.start();
		try {
			zw.t.join();
			wt.t.join();
		}catch(InterruptedException e) {
			System.out.println("Main thread interrupted");
		}

	}
}
class ZoneWriter implements Runnable{
	AscHeader h;
	Thread t;
	String fn;
	int ri;
	ZoneWriter(AscHeader header, String filename,  String region_id ){
		h = header;
		fn = filename;
		ri = Integer.parseInt(region_id);
		t = new Thread(this);
	}
	public void run() {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn), "UTF-8"))){
			for(int i=0; i<h.ncols;i+=15) {
				for(int j=0; j<h.nrows; j+=15) {
					int x = (int)(h.llon/h.cellsize)+ i + 8;
					int y = (int)(h.llat/h.cellsize)+ j + 8;
					double lat = h.llat+(h.cellsize*j);
					double lon = h.llon+(h.cellsize*i);
					bw.write("Zone.create(region_id:"+ri+", place:'"+x+"_"+y+"', lat:"+lat+", lon:"+lon+")\n");
				}
			}
			bw.close();
		}catch(IOException e) {
			System.out.println("I/O error"+e);
		}
			
	}
}
class WriterTxt implements Runnable{
	AscHeader h;
	Thread t;
	String fn;
	int ri;
	WriterTxt(AscHeader header, String filename,  String region_id ){
		h = header;
		fn = filename;
		ri = Integer.parseInt(region_id);
		t = new Thread(this);
	}
	public void run() {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn), "UTF-8"))){
			int counter=0;
			for(int i=0; i<h.ncols;i+=15) {
				for(int j=0; j<h.nrows; j+=15) {
					int x = (int)(h.llon/h.cellsize)+ i + 8;
					int y = (int)(h.llat/h.cellsize)+ j + 8;
					double lat = h.llat+(h.cellsize*j);
					double lon = h.llon+(h.cellsize*i);					
					counter++;
					bw.write(counter+" "+lat+" "+lon+" "+x+"_"+y+"\n");
				}
			}
			bw.close();
		}catch(IOException e) {
			System.out.println("I/O error"+e);
		}
			
	}
}
class AscHeader{
	int ncols, nrows; // number of columns and rows;
	double llat, llon, cellsize, noData;
	CellRank[] cr;
	
	AscHeader(String fileName){
		String[] strHeader = new String[6];
		try(BufferedReader br = new BufferedReader(new FileReader(fileName))){			
			for(int i=0; i<6; i++) {
				strHeader[i] = br.readLine();
			}
		}catch(IOException e) {
			System.out.println("I/O Error: "+e);
		}
		String[] d1=strHeader[0].split(" ");
		ncols=Integer.parseInt(d1[d1.length-1]);
		String[] d2=strHeader[1].split(" ");
		nrows=Integer.parseInt(d2[d2.length-1]);	
		String[] d3=strHeader[2].split(" ");
		llon=Double.parseDouble(d3[d3.length-1]);		
		String[] d4=strHeader[3].split(" ");
		llat=Double.parseDouble(d4[d4.length-1]);		
		String[] d5=strHeader[4].split(" ");
		cellsize=Double.parseDouble(d5[d5.length-1]);
		String[] d6=strHeader[5].split(" ");
		noData=Double.parseDouble(d6[d6.length-1]);
		cr = new CellRank[ncols*nrows];
		for(int i=0; i<(ncols*nrows); i++) {
			cr[i]=new CellRank();
		}
	}	
}
