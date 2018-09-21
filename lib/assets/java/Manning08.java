package bin;
import java.io.*;

class Geodesic{
	double distance(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6371000;
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
				Math.sin(dLon/2)*Math.asin(dLon/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		float dist = (float)(earthRadius*c);
		return dist; // in meters
	}
}

class AscHeader{
	int ncols, nrows; // number of columns and rows;
	double llat, llon, cellsize, noData;
	
	AscHeader(){
	}
	
	AscHeader(String[] lineHeader){
		if(lineHeader.length!=6){
			System.out.println("Header incorrect");
		}else{
			String[] d1=lineHeader[0].split(" ");
			ncols=Integer.parseInt(d1[d1.length-1]);
			
			String[] d2=lineHeader[1].split(" ");
			nrows=Integer.parseInt(d2[d2.length-1]);
			
			String[] d3=lineHeader[2].split(" ");
			llon=Double.parseDouble(d3[d3.length-1]);
			
			String[] d4=lineHeader[3].split(" ");
			llat=Double.parseDouble(d4[d4.length-1]);
			
			String[] d5=lineHeader[4].split(" ");
			cellsize=Double.parseDouble(d5[d5.length-1]);

			String[] d6=lineHeader[5].split(" ");
			noData=Double.parseDouble(d6[d6.length-1]);
		}
	}
}

class FileSaving{
	FileSaving(){
		
	}	
	void Saving(Cell[][] cell, String fileName, AscHeader fh){
		try {
		
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			writer.write("ncols "+fh.ncols+"\n");
			writer.write("nrows "+fh.nrows+"\n");
			writer.write("xllcorner "+fh.llon+"\n");
			writer.write("yllcorner "+fh.llat+"\n");
			writer.write("cellsize "+fh.cellsize+"\n");
			writer.write("NODATA_value"+fh.noData+"\n");
			for(int j=fh.nrows-1; j>=0; j--) {
				String line="";
				for(int i=0; i<fh.ncols;i++) {
					line+=cell[i][j].zW+" ";
					
				}
				line+="\n";
				writer.write(line);		     
			}
			writer.close();
		}catch(IOException e) {
			System.out.println("I/O Error:"+e);
		}		
	}
	void Saving(double[][] dm, String fileName, AscHeader fh){
		try {
		
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			writer.write("ncols "+fh.ncols+"\n");
			writer.write("nrows "+fh.nrows+"\n");
			writer.write("xllcorner "+fh.llon+"\n");
			writer.write("yllcorner "+fh.llat+"\n");
			writer.write("cellsize "+fh.cellsize+"\n");
			writer.write("NODATA_value "+fh.noData+"\n");
			for(int j=fh.nrows-1; j>=0; j--) {
				String line="";
				for(int i=0; i<fh.ncols;i++) {
					line+=dm[i][j]+" ";
					
				}
				line+="\n";
				writer.write(line);		     
			}
			writer.close();
		}catch(IOException e) {
			System.out.println("I/O Error:"+e);
		}		
	}

}

class Cell {
	double vel; 		  // speed of water
	double[] svel;
	double xS, yS, zS; // surface dimension
	double[] sxS, syS, szS;
	double zW;         // deep of water
	double zW0;
	double[] szW;         // deep of water
	//double[] rf;        // exceeded from where
	double wca,n,vol;
	double lat,lon;
	Cell(){	
		sxS=new double[8];
		syS=new double[8];
		szS=new double[8];
		szW=new double[8];
		svel=new double[8];
		//rf = new double[8];
		//for(int k=0; k<8; k++) {
		//	rf[k]=-1;
		//}
	}
	
	double getTotalsvel() {
		double tsvel=0;
		for(int k=0; k<8; k++) {
			tsvel+=svel[k];
		}
		return tsvel;
	}
}
class Direction{
	int i[] ={-1,0,1,-1,1,-1,0,1};
	int j[] ={-1,-1,-1,0,0,1,1,1};
	//int k[] ={7,6,5,4,3,2,1,0};
	
	Direction(){		
	}
}

class Manning implements Runnable{
	Cell c; // input cell;
	double noData;
	Thread t;
	
	Manning(Cell cell){
		c=cell;
		t=new Thread(this);
	}
	
	public void run() {
		for(int k=0; k<8;k++) {
			//manning calculation
			double S=0; 
			double wide=c.xS;
			double zS, szS;
			
			if (c.zS!=noData) {
				zS=0;
			}else {
				zS=c.zS;
			}
			if (c.szS[k]!=noData) {
				szS=0;
			}else {
				szS=c.szS[k];
			}
							
			S = ((zS+c.zW)-(szS+c.szW[k])) /wide;
						
			double A = 0;
			//System.out.println(zS+" "+c.zW+" "+szS+" "+c.szW[k]);
			if(S>0) {
				double const1= (double)2/(double)3;		
				A = wide*c.zW;
				double P = wide+(2*c.zW);
				double const2 = (double)A/(double)P;
				c.svel[k] = (1/c.n) *
						Math.pow(const2, const1) *
						Math.pow(S,0.5);
				//System.out.println(A);
			} else {
				c.svel[k]=0;
			}			
		}
	}
}

class Manning08{
	public static void main(String args[]) {
		// File Asc
		// File result
		// Wave long
		// Wave height
		Direction dir = new Direction();
		double n = 0.014;//Double.parseDouble((args[4]);
		
		
		// get header >>>
		AscHeader fh = new AscHeader();
		try (BufferedReader br = new BufferedReader(new FileReader(args[0]))){
			
			// Initializing
			String[] strHeader = new String[6];
			for(int i=0; i<6; i++) {
				strHeader[i] = br.readLine();
			}
			fh=new AscHeader(strHeader);
		}catch(IOException e) {
			System.out.println("I/O Error:"+e);
		}				
		// get header <<<
		// define dangerous matrix >>>
		double[][] dMatrix= new double[fh.ncols][];
		for(int i=0; i<fh.ncols;i++) {
			dMatrix[i]= new double[fh.nrows];			
		}
		// define dangerous matrix <<<		
		// assign cells >>>
		System.out.print("Cells initializing -> ");
		Cell[][] cell=new Cell[fh.ncols][];		
		for(int i=0; i<fh.ncols; i++) {
			cell[i]=new Cell[fh.nrows];
			for (int j=0; j<fh.nrows; j++) {
				cell[i][j] = new Cell();
			}
		}

		try(BufferedReader br = new BufferedReader(new FileReader(args[0]))){
			// skip header >>>
			for(int i=0; i<6; i++) {
				String skipHeader = br.readLine();
			}
			// skip header <<<			
			for(int j=fh.nrows-1; j>=0; j--) {
				String[] d1Line = br.readLine().split(" ");
				for(int i=0; i<fh.ncols; i++) {
					cell[i][j].zS=Double.parseDouble(d1Line[i]);
					cell[i][j].lat = (double)fh.llat + (double)fh.cellsize * (double)j;
					cell[i][j].lon = (double)fh.llon + (double)fh.cellsize * (double)i;
					Geodesic geo = new Geodesic();
					cell[i][j].yS = (double)geo.distance(cell[i][j].lat, cell[i][j].lon, cell[i][j].lat+fh.cellsize, cell[i][j].lon);
					cell[i][j].xS = (double)geo.distance(cell[i][j].lat, cell[i][j].lon, cell[i][j].lat, cell[i][j].lon+fh.cellsize);
					cell[i][j].n = (double)n;
				}
			}				
		}catch(IOException e) {
			System.out.println("I/O Error:"+ e);
		}
		System.out.println("Completed");
		// assign cell <<<
		
		// Determining neighborhood >>>
		for(int i=0; i<fh.ncols;i++) {
			for(int j=0; j<fh.nrows;j++) {
				for(int k=0;k<8;k++) {
					if((i+dir.i[k])>=0 && (i+dir.i[k])<fh.ncols && (j+dir.j[k])>=0 && (j+dir.j[k])<fh.nrows) {
						cell[i][j].szS[k]=cell[i+dir.i[k]][j+dir.j[k]].zS;
						cell[i][j].szW[k]=cell[i+dir.i[k]][j+dir.j[k]].zW;
					}
				}
			}
		}
		// Determining neighborhood <<<
		
		// Main Iteration >>>
		//int seconds = Integer.parseInt(args[2]);
		int second=0;
		double wavelong = Double.parseDouble(args[2]);
		double waveheight = Double.parseDouble(args[3]);
		boolean findone=false;
		do {
		//for(int second=0; second<=seconds; second++) {
			// watering
			if (wavelong>0) {
				for(int i=0; i<fh.ncols; i++) {
					for(int j=0; j<fh.nrows; j++) {
						if (cell[i][j].zS==fh.noData) {
							cell[i][j].zW=waveheight;
							cell[i][j].vel=cell[i][j].xS;
						}
					}
				}
			}
			//
			for(int i=0; i<fh.ncols; i++) {
				// Manning calculation >>>
				Manning[] man = new Manning[fh.nrows];
				int[] idReg=new int[0];
				for(int j=0; j<fh.nrows; j++) {					
					if (cell[i][j].vel>=cell[i][j].xS) {
						// check surround water >>>
						for(int k=0;k<8;k++) {
							if((i+dir.i[k])>=0 && (i+dir.i[k])<fh.ncols && (j+dir.j[k])>=0 && (j+dir.j[k])<fh.nrows) {
								cell[i][j].szW[k]=cell[i+dir.i[k]][j+dir.j[k]].zW;
							}
						}
						// check surround water <<<
						cell[i][j].vel=cell[i][j].xS;
						idReg = (int[])resizeArray(idReg,idReg.length+1);
						idReg[idReg.length-1]=j;
						man[j]= new Manning(cell[i][j]);
						man[j].t.start();
					}
				}
				// Manning calculation <<<
				// waiting manning paralel calcualtion >>>
				try {
					for(int j = 0; j<idReg.length; j++) {
						man[idReg[j]].t.join();
					}
					//for(int i=0; i<fh.ncols;i++) {
					//	man[i].t.join();
					//}
				}catch(InterruptedException e) {
					System.out.println("Main thread interrupted");
				}				
				// waiting manning paralel calcualtion <<<												
			}
			for(int i=0; i<fh.ncols; i++) {
				for(int j=0; j<fh.nrows; j++) {
					cell[i][j].zW0=cell[i][j].zW;
				}
			}
			// distributing velocity >>>
			for(int i=0; i<fh.ncols; i++) {
				for(int j=0; j<fh.nrows; j++) {
					boolean uwd=false; // urgent water distribution
					for(int k=0; k<8; k++) {
						if((i+dir.i[k])>=0 && (i+dir.i[k])<fh.ncols && (j+dir.j[k])>=0 && (j+dir.j[k])<fh.nrows) {							
							if(cell[i+dir.i[k]][j+dir.j[k]].vel < cell[i][j].svel[k]) {
								cell[i+dir.i[k]][j+dir.j[k]].vel=cell[i][j].svel[k];
								
								// if exceeded
								if (cell[i][j].svel[k]>cell[i+dir.i[k]][j+dir.j[k]].xS) {
									//cell[i+dir.i[k]][j+dir.j[k]].rf[k]=dir.k[k];
									uwd=true;
								}
							}
						}
					}
				}
			}			
			
			// water distributing >>>
			for (int i=0; i<fh.ncols;i++) {
				for(int j=0; j<fh.nrows;j++) {
					double tvel= cell[i][j].getTotalsvel();
					for(int k=0; k<8;k++) {							
						if((i+dir.i[k])>=0 && (i+dir.i[k])<fh.ncols && (j+dir.j[k])>=0 && (j+dir.j[k])<fh.nrows) {

							if(cell[i+dir.i[k]][j+dir.j[k]].zS!=fh.noData && cell[i][j].zW0>0 ) {
								if (tvel>=cell[i][j].xS) {
									cell[i+dir.i[k]][j+dir.j[k]].zW+= cell[i][j].svel[k]/tvel*cell[i][j].zW0;
								}else {
									double disWater= tvel/cell[i][j].xS*cell[i][j].zW0;
									if (disWater>0) {
										cell[i+dir.i[k]][j+dir.j[k]].zW+= cell[i][j].svel[k]/tvel*disWater;
									}
								}									
							}
						}
					}
					//System.out.println(cell[i][j].zW);
				}
			}
			
			// water distributing <<<
			// water reducing >>>
			findone=false;
			for (int i=0; i<fh.ncols;i++) {
				for(int j=0; j<fh.nrows;j++) {
					if (cell[i][j].zW>0 ) {
						boolean sea=true;
						for(int k=0; k<8; k++) {
							if(cell[i][j].szS[k]!=fh.noData) {
								sea=false;
							}						
						}
						if (sea==false) {
							dMatrix[i][j]+=1;
						}
					}
					if ((cell[i][j].zW+cell[i][j].zS)>waveheight) {
						cell[i][j].zW=waveheight-cell[i][j].zS;
						if (cell[i][j].zW<0)  cell[i][j].zW=0;
					}
					if (dMatrix[i][j]==1) findone=true;
				}
			}
			// water reducing <<<
			FileSaving fs = new FileSaving();		
			fs.Saving(dMatrix, args[1]+second+".asc", fh);
			wavelong-=1;
			second++;
			System.out.println(second+" seconds simulation completed");
		}while(findone);
		
		// Main Iteration <<<
		
		// saving file >>>
		//FileSaving fs = new FileSaving();		
		//fs.Saving(cell, args[1], fh);
		//fs.Saving(dMatrix, args[1], fh);
		// saving file <<<
		
	}
	
	// resize array function
	private static Object resizeArray (Object oldArray, int newSize) {
		int oldSize = java.lang.reflect.Array.getLength(oldArray);
		Class elementType = oldArray.getClass().getComponentType();
		Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
		int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0) System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
		return newArray; 
	}
}
