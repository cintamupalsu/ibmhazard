package bin;
import java.io.*;
import java.text.DecimalFormat;
// 1. ASC file
// 2. ASC result (in series) 
// 3. Rain Time (in sec)
// 4. Rain Intensity (in mm)
// 5. Simulation Time ( in sec)
// 6. 0: River flow simulation 1: Landslide calculation
class Direction{
	int i[] ={-1, 0, 1,-1,1,-1,0,1};
	int j[] ={-1,-1,-1, 0,0, 1,1,1};
	//int k[] ={7,6,5,4,3,2,1,0};
	
	Direction(){		
	}
}

class CellRank{
	double Z;
	int x,y;
	CellRank(){
		Z=-9999;
		x=-1;
		y=-1;
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

class AscWrite implements Runnable{
	AscCell[][] c;
	String fn;
	AscHeader fh;
	int iter;
	Thread t;
	
	AscWrite(){
		t = new Thread(this);
	}
	AscWrite(AscCell[][] cells, String fileName, AscHeader fileHeader, int iterationNo){
		c = cells;
		fn = fileName;
		fh = fileHeader;
		iter= iterationNo;
		t = new Thread(this);
	}
	public void run() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fn+iter+".asc"))){
			
			bw.write("ncols "+fh.ncols+"\n");
			bw.write("nrows "+fh.nrows+"\n");
			bw.write("xllcorner "+fh.llon+"\n");
			bw.write("yllcorner "+fh.llat+"\n");
			bw.write("cellsize "+fh.cellsize+"\n");
			bw.write("NODATA_value "+fh.noData+"\n");
			
			for(int j=fh.nrows-1; j>=0; j--) {
				String line="";
				for(int i=0; i<fh.ncols;i++) {
					line+=c[i][j].z[0][1][1] +" ";					
				}
				line+="\n";
				bw.write(line);		     
			}
			bw.close();
		}catch(IOException e) {
			System.out.println("I/O Error: "+e);
		}
		
	}
}

class AscWriteArray implements Runnable{
	double[][] c;
	String fn;
	AscHeader fh;
	int iter;
	Thread t;
	
	AscWriteArray(){
		t = new Thread(this);
	}
	AscWriteArray(double[][] cells, String fileName, AscHeader fileHeader, int iterationNo){
		c = cells;
		fn = fileName;
		fh = fileHeader;
		iter= iterationNo;
		t = new Thread(this);
	}
	public void run() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fn+iter+".asc"))){
			
			bw.write("ncols "+fh.ncols+"\n");
			bw.write("nrows "+fh.nrows+"\n");
			bw.write("xllcorner "+fh.llon+"\n");
			bw.write("yllcorner "+fh.llat+"\n");
			bw.write("cellsize "+fh.cellsize+"\n");
			bw.write("NODATA_value "+fh.noData+"\n");
			
			for(int j=fh.nrows-1; j>=0; j--) {
				String line="";
				for(int i=0; i<fh.ncols;i++) {
					line+=c[i][j] +" ";					
				}
				line+="\n";
				bw.write(line);		     
			}
			bw.close();
		}catch(IOException e) {
			System.out.println("I/O Error: "+e);
		}
		
	}
}

class AscCell{
	double Z[][], X[][], Y[][], V[][];
	double z[][][];
	
	double n;
	
	AscCell(double n){
		this.n = n;
		// Water height declaration 
		this.z = new double[2][][];				
		for(int a=0; a<2; a++) {
			z[a] = new double[3][]; 
			for(int i=0; i<3;i++) {
				z[a][i]= new double[3]; 
			}
		}
		// Surface declaration
		this.X = new double[3][];
		this.Y = new double[3][];
		this.Z = new double[3][];
		this.V = new double[3][];
		for(int i=0; i<3; i++) {
			X[i] = new double[3];
			Y[i] = new double[3];
			Z[i] = new double[3];
			V[i] = new double[3];
		}
	}
}

class ManningCalculation implements Runnable{
	AscCell c;
	int cn;
	Thread t;
	int md;    // 0: Normal, 1: Tsunami Happen Z sea = 0
	double nd; // noData
	ManningCalculation(AscCell aCell, int mode, double noData, int crNo) {
		c = aCell;
		md = mode;
		nd=noData;
		cn=crNo;
		t = new Thread(this);
		
	}
	
	public void run() {
		double[] P = new double[8];
		double[] Q = new double[8]; // Q Theoritical			
		double TQ = 0;    // total Q
		Direction d = new Direction();
		
		
		P[0] = (0.5*(c.X[1][1]+c.Y[1][1]))+(2*c.z[0][1][1]);
		P[2] = P[0];
		P[5] = P[0];
		P[7] = P[0];
		P[1] = c.X[1][1]+(2*c.z[0][1][1]);
		P[6] = P[1];
		P[3] = c.Y[1][1]+(2*c.z[0][1][1]);
		P[4] = P[3];		
		
		for(int k=0; k<8; k++) {

			double S =0;
			if(md==0) {
				S = (c.Z[1][1] - c.Z[1+d.i[k]][1+d.j[k]] - c.z[0][1+d.i[k]][1+d.j[k]] + c.z[0][1][1]) / (P[k]-(2*c.z[0][1][1]));
			}
			if(md==1) {
				double Z0=c.Z[1][1];
				double Z1=c.Z[1+d.i[k]][1+d.j[k]];
				if (Z0==nd) {
					Z0=0;
				}
				if (Z1==nd) {
					Z1=0;
				}
				S = (Z0 - Z1 - c.z[0][1+d.i[k]][1+d.j[k]] + c.z[0][1][1]) / (P[k]-(2*c.z[0][1][1]));
			}
			double A = (P[k]-(2*c.z[0][1][1])) * c.z[0][1][1];		

			if (S>0) {
				c.V[1+d.i[k]][1+d.j[k]] = (1.0/c.n) * Math.pow(A/P[k], 2.0/3.0) * Math.pow(S, 0.5);
				Q[k] = c.V[1+d.i[k]][1+d.j[k]] * A;
			
				TQ+=Q[k];
			}else {
				Q[k]=0;
				c.V[1+d.i[k]][1+d.j[k]]=0;
			}
		}
		double vol0= c.X[1][1] * c.Y[1][1] * c.z[0][1][1];
		double ratio = 1;
		if (TQ<vol0 ) {
			ratio = TQ / vol0;
		}

		
		c.z[1][1][1] =c.z[0][1][1];
		for(int k=0; k<8; k++) {
			if (TQ>0) {
				c.z[1][1+d.i[k]][1+d.j[k]] = (Q[k]/TQ * vol0 * ratio) / (c.X[1][1] * c.Y[1][1]);
			}else {
				c.z[1][1+d.i[k]][1+d.j[k]] = 0;
			}
			c.z[1][1][1]=c.z[1][1][1]-c.z[1][1+d.i[k]][1+d.j[k]];
		}		
	}
}

class SoilAbsorption {
	double[][] abwater; // absorbed water
	double[][] force;   // force on each cell
	double ir; // infiltration / absorption rate
	Direction d;
	SoilAbsorption (AscHeader fileHeader, double infilRate) {
		d = new Direction();
		this.abwater = new double[fileHeader.ncols][];
		this.force = new double[fileHeader.ncols][];
		for(int i=0; i<fileHeader.ncols; i++) {
			abwater[i] = new double[fileHeader.nrows];
			force[i] = new double[fileHeader.nrows];
		}
		ir=infilRate;
	}
	void AbsorbWater(AscCell[][] c) {
	
		for(int i = 0; i < c.length; i++) {
			for(int j=0; j<c[0].length;j++) {
				
				// get biggest force				
				for(int k=0;k<8; k++) {
					double y = Math.abs(c[i][j].Z[1][1]-c[i][j].Z[1+d.i[k]][1+d.j[k]]);
					double x = (c[i][j].X[1][1]+c[i][j].Y[1][1])/2;
					double volume = c[i][j].X[1][1]*c[i][j].Y[1][1]*c[i][j].z[0][1][1];
					abwater[i][j] += (volume * ir);
					//System.out.println(x+" "+y+" "+volume);
					double f=ForceCalculator(x,y,abwater[i][j]);
					if(f>force[i][j]) {
						force[i][j]=f;
					}
				}
				// end of get biggest force
				//System.out.println(force[i][j]);
				
			}
		}
		//double f = ForceCalculator(a,b,volume);	
		//System.out.println(f);		
	}
	double ForceCalculator(double x, double y, double volume) { // volume in meter cubic
		double lean = Math.sqrt(x*x+y*y);
		double tetha = Math.asin(y/lean)*57.295779513;
		double g = 9.83;
		double m=1000*volume;
		double f=m*g*Math.sin(Math.toRadians(tetha));
		return f;
	}
}

class Manning10RLs{
	public static void main(String args[]) {
		AscHeader fh = new AscHeader(args[0]);
		AscCell[][] c = new AscCell[fh.ncols][];
		AscCell[][] wc = new AscCell[fh.ncols][];
		double n = 0.014;
		double infil_rate=0.004;
		
		SoilAbsorption sa;
		
		for(int i=0; i<fh.ncols; i++) {
			c[i] = new AscCell[fh.nrows];
			wc[i]= new AscCell[fh.nrows];
			for(int j=0; j<fh.nrows; j++) {
				c[i][j]= new AscCell(n);
				wc[i][j] = new AscCell(infil_rate);
			}
		}
		sa= new SoilAbsorption(fh, infil_rate);
		//sa.AbsorbWater();
		
		// Assign value to AscCells >>>
		try(BufferedReader br = new BufferedReader(new FileReader(args[0]))){
			// Skip header >>>
			for(int i=0; i<6; i++) {
				String skipHeader = br.readLine();
			}
			// Skip header <<<
			
			// Assign Surface >>>
			System.out.print("Read Surface data =>");

			Geodesic geo = new Geodesic();
			int countcr=0;
			
			for(int j=fh.nrows-1; j>=0; j--) {					
				String[] d1Line = br.readLine().split(" ");
				for(int i=0; i<fh.ncols;i++) {					
					c[i][j].Z[1][1]=Double.parseDouble(d1Line[i]);					
					c[i][j].Y[1][1]=geo.distance(fh.llat+j*fh.cellsize,fh.llon+i*fh.cellsize,fh.llat+(j+1)*fh.cellsize,fh.llon+i*fh.cellsize);	
					c[i][j].X[1][1]=geo.distance(fh.llat+j*fh.cellsize,fh.llon+i*fh.cellsize,fh.llat+j*fh.cellsize,fh.llon+(i+1)*fh.cellsize);									
					
					fh.cr[countcr].Z=Double.parseDouble(d1Line[i]);					
					fh.cr[countcr].x=i;
					fh.cr[countcr].y=j;
					countcr++;
				}
			}					
			System.out.println(" Completed");
			// Assign Surface <<<
			
			// sort cr >>>
			System.out.print("Indexing Surface data =>");
			for(int i=0; i< fh.cr.length-1; i++) {
				for(int j=i+1; j<fh.cr.length;j++) {
					if (fh.cr[i].Z<fh.cr[j].Z) {
						CellRank cr = new CellRank();
						cr = fh.cr[i];
						fh.cr[i]=fh.cr[j];
						fh.cr[j]=cr;
					}
				}
			}
			System.out.println(" Completed");
			// sort cr <<<
			
			// Init Surface >>>
			System.out.print("Cells connecting =>");
			Direction d= new Direction();
			for(int i=0; i<fh.ncols; i++) {
				for(int j=0; j< fh.nrows; j++) {
					for(int k=0; k<d.i.length; k++) {
						if((i+d.i[k]>=0) && (i+d.i[k]<fh.ncols) && (j+d.j[k]>=0) && (j+d.j[k]<fh.nrows)) {
							c[i][j].Z[1+d.i[k]][1+d.j[k]] = c[i+d.i[k]][j+d.j[k]].Z[1][1];
							c[i][j].X[1+d.i[k]][1+d.j[k]] = c[i+d.i[k]][j+d.j[k]].X[1][1];
							c[i][j].Y[1+d.i[k]][1+d.j[k]] = c[i+d.i[k]][j+d.j[k]].Y[1][1];
						} else {
							c[i][j].Z[1+d.i[k]][1+d.j[k]] = c[i][j].Z[1][1];
							c[i][j].X[1+d.i[k]][1+d.j[k]] = c[i][j].X[1][1];
							c[i][j].Y[1+d.i[k]][1+d.j[k]] = c[i][j].Y[1][1];
						}
					}
				}
			}	
			wc=c;
			System.out.print(" Completed");
			// Init Surface <<<
									
		}catch(IOException e) {
			System.out.println("I/O Error: "+e);
		}		
		// Assign value to AscCells <<<
		
		// Rain iteration >>>
		System.out.println("Rain iteration");
		String fn = args[1];
		int rainTime=Integer.parseInt(args[2]);
		double intensity=Double.parseDouble(args[3]);
		int simuTime=Integer.parseInt(args[4]);
		AscWrite aw = new AscWrite();		
		Direction d= new Direction();
		AscWriteArray awa = new AscWriteArray();
		
		int iCPU = Runtime.getRuntime().availableProcessors();
		System.out.println("Calculating using "+iCPU+" processors =>");
		for(int second=0; second<=simuTime; second++) {
			// If still raining >>>
			if(second<=rainTime) {
				for(int i=0; i<fh.ncols; i++) {
					for(int j=0; j<fh.nrows; j++) {
						c[i][j].z[0][1][1] += intensity;
						for(int k=0; k<8 ;k++) {
							c[i][j].z[0][1+d.i[k]][1+d.j[k]] += intensity;
						} // end of for k
					} // end of for j
				} // end of for i
			} // end if
			// if still raining <<<
			
			
			// Manning calculation >>>
			int usedCPU = 0;
			ManningCalculation[] mc = new ManningCalculation[iCPU];
			//for(int i=0; i<fh.cr.length; i++) {
			for(int i=fh.cr.length-1; i>=0; i--) {
				mc[usedCPU] = new ManningCalculation(c[fh.cr[i].x][fh.cr[i].y], 0,fh.noData, i);
				mc[usedCPU].t.start();
				usedCPU++;
				if (usedCPU>=iCPU) {
					try {
						for(int j=0; j<iCPU; j++) {
							mc[j].t.join();
						}
						// update circumstance z >>>
						for(int j=0; j<iCPU; j++) {
							for(int k=0; k<8;k++) {
								int ori_i = fh.cr[mc[j].cn].x;
								int ori_j = fh.cr[mc[j].cn].y;
								int upt_i = ori_i+d.i[7-k];
								int upt_j = ori_j+d.j[7-k];
								//System.out.print(ori_i+"-"+ori_j+" "+upt_i+" "+upt_j+" | ");
								if((upt_i>=0) && (upt_i<fh.ncols) && (upt_j>=0) && (upt_j<fh.nrows)) {
									c[upt_i][upt_j].z[0][1+d.i[k]][1+d.j[k]]=c[ori_i][ori_j].z[1][1][1];
								}
							}
							//System.out.println();
						}
						// update circumstance z <<<
					}catch(InterruptedException e) {
						System.out.println("Main thread interrupted");
					}
					usedCPU=0;
				}				
			}
			
			try {
				for(int j=0; j<usedCPU; j++) {
					mc[j].t.join();
				}
			}catch(InterruptedException e) {
				System.out.println("Main thread interrupted");
			}
			usedCPU=0;
			// Manning calculation <<<
			
			//// 1 => 0 >>>
			//for(int i=0; i<fh.ncols; i++) {
			//	
			//	for(int j=0; j<fh.nrows; j++) {
			//		c[i][j].z[0][1][1] = c[i][j].z[1][1][1];
			////		System.out.println(c[i][j].z[1][1][1]);
			//	}
			////	
			//}
			//System.out.println(c[1][fh.nrows-2].z[0][0][2]+" "+c[1][fh.nrows-2].z[0][1][2]+" "+c[1][fh.nrows-2].z[0][2][2]);
			//System.out.println(c[1][fh.nrows-2].z[0][0][1]+" "+c[1][fh.nrows-2].z[0][1][1]+" "+c[1][fh.nrows-2].z[0][2][1]);
			//System.out.println(c[1][fh.nrows-2].z[0][0][0]+" "+c[1][fh.nrows-2].z[0][1][0]+" "+c[1][fh.nrows-2].z[0][2][0]);
			//System.out.println("-----------------");
			////System.out.println(c[1][fh.nrows-2].z[1][0][2]+" "+c[1][fh.nrows-2].z[1][1][2]+" "+c[1][fh.nrows-2].z[1][2][2]);
			////System.out.println(c[1][fh.nrows-2].z[1][0][1]+" "+c[1][fh.nrows-2].z[1][1][1]+" "+c[1][fh.nrows-2].z[1][2][1]);
			////System.out.println(c[1][fh.nrows-2].z[1][0][0]+" "+c[1][fh.nrows-2].z[1][1][0]+" "+c[1][fh.nrows-2].z[1][2][0]);
			//for(int i=0; i<fh.ncols; i++) {
			//	for(int j=0; j<fh.nrows; j++) {
			//		for(int k=0; k<8; k++) {
			//			if((i+d.i[k]>=0) && (i+d.i[k]<fh.ncols) && (j+d.j[k]>=0) && (j+d.j[k]<fh.nrows)) {
			//				c[i+d.i[k]][j+d.j[k]].z[0][1][1] += c[i][j].z[1][1+d.i[k]][1+d.j[k]];
			//				c[i][j].z[1][1+d.i[k]][1+d.j[k]]=0;						
			//			}
			//		}					
			//	}
			//}
			//for(int i=0; i<fh.ncols; i++) {
			//	for(int j=0; j<fh.nrows; j++) {
			//		for(int k=0; k<8; k++) {
			//			if((i+d.i[k]>=0) && (i+d.i[k]<fh.ncols) && (j+d.j[k]>=0) && (j+d.j[k]<fh.nrows)) {
			//				c[i][j].z[0][1+d.i[k]][1+d.j[k]]=c[i+d.i[k]][j+d.j[k]].z[0][1][1];
			//			}
			//		}
			//	}
			//}
			for(int h=0; h<fh.cr.length; h++) {
				int i=fh.cr[h].x;
				int j=fh.cr[h].y;
				c[i][j].z[0][1][1]=c[i][j].z[1][1][1];
				c[i][j].z[1][1][1]=0;
			}
			for(int h=0; h<fh.cr.length; h++) {
				int i=fh.cr[h].x;
				int j=fh.cr[h].y;
				for(int k=0; k<8; k++) {
					if((i+d.i[k]>=0) && (i+d.i[k]<fh.ncols) && (j+d.j[k]>=0) && (j+d.j[k]<fh.nrows)) {
						c[i+d.i[k]][j+d.j[k]].z[0][1][1]+=c[i][j].z[1][1+d.i[k]][1+d.j[k]];
					}
					c[i][j].z[1][1+d.i[k]][1+d.j[k]]=0;
				}
			}
			for(int h=0; h<fh.cr.length; h++) {
				int i=fh.cr[h].x;
				int j=fh.cr[h].y;
				for(int k=0; k<8; k++) {
					if((i+d.i[k]>=0) && (i+d.i[k]<fh.ncols) && (j+d.j[k]>=0) && (j+d.j[k]<fh.nrows)) {
						c[i][j].z[0][1+d.i[k]][1+d.j[k]]=c[i+d.i[k]][j+d.j[k]].z[0][1][1];
					}else {
						c[i][j].z[0][1+d.i[k]][1+d.j[k]]=c[i][j].z[0][1][1];
					}
				}
			}
			
			//// 1 => 0 <<<
			System.out.println("-----------------");
			System.out.println(c[1][fh.nrows-2].z[0][0][2]+" "+c[1][fh.nrows-2].z[0][1][2]+" "+c[1][fh.nrows-2].z[0][2][2]);
			System.out.println(c[1][fh.nrows-2].z[0][0][1]+" "+c[1][fh.nrows-2].z[0][1][1]+" "+c[1][fh.nrows-2].z[0][2][1]);
			System.out.println(c[1][fh.nrows-2].z[0][0][0]+" "+c[1][fh.nrows-2].z[0][1][0]+" "+c[1][fh.nrows-2].z[0][2][0]);
			// set 0 for sea >>>
			for(int i=0; i<fh.ncols; i++) {				
				for(int j=0; j<fh.nrows; j++) {
					if(c[i][j].Z[1][1]==fh.noData) {
						c[i][j].z[0][1][1] = 0;
					}
				}
			}
			// set 0 for sea <<<
			int rivermode= Integer.parseInt(args[5]);
			if (rivermode==0) {
			
				if (aw.t.isAlive()) {
					try {
						aw.t.join();
					}catch(InterruptedException e) {
						System.out.println("Main thread interrupted");
					}
				}
				aw = new AscWrite(c,fn, fh, second);
				aw.t.start();
				System.out.println("Riverflowmode");
			}	
			if (rivermode==1) {
				if (awa.t.isAlive()) {
					try {
						awa.t.join();
					}catch(InterruptedException e) {
						System.out.println("Main thread interrupted");
					}
				}
				sa.AbsorbWater(c);
				awa = new AscWriteArray(sa.force,fn,fh,second);
				awa.t.start();
				System.out.println("Landslidemode");
			}
			System.out.println(second+" seconds rain simulation done");
		}		
		// Rain iteration <<<
		try {
			System.out.println("Closing");
			aw.t.join();
		
		}catch(InterruptedException e) {
			System.out.println("Main thread interrupted");
		}
		
		for (int i=0; i<fh.cr.length; i++) {
			//System.out.println(fh.cr[i].Z+" "+fh.cr[i].X+" "+fh.cr[i].Y);
		}
		
		System.out.println(c[1][fh.nrows-2].Z[0][2]+" "+c[1][fh.nrows-2].Z[1][2]+" "+c[1][fh.nrows-2].Z[2][2]);
		System.out.println(c[1][fh.nrows-2].Z[0][1]+" "+c[1][fh.nrows-2].Z[1][1]+" "+c[1][fh.nrows-2].Z[2][1]);
		System.out.println(c[1][fh.nrows-2].Z[0][0]+" "+c[1][fh.nrows-2].Z[1][0]+" "+c[1][fh.nrows-2].Z[2][0]);

		
		System.out.println(c[1][fh.nrows-2].z[0][0][2]+" "+c[1][fh.nrows-2].z[0][1][2]+" "+c[1][fh.nrows-2].z[0][2][2]);
		System.out.println(c[1][fh.nrows-2].z[0][0][1]+" "+c[1][fh.nrows-2].z[0][1][1]+" "+c[1][fh.nrows-2].z[0][2][1]);
		System.out.println(c[1][fh.nrows-2].z[0][0][0]+" "+c[1][fh.nrows-2].z[0][1][0]+" "+c[1][fh.nrows-2].z[0][2][0]);

		System.out.println(c[1][fh.nrows-2].z[1][0][2]+" "+c[1][fh.nrows-2].z[1][1][2]+" "+c[1][fh.nrows-2].z[1][2][2]);
		System.out.println(c[1][fh.nrows-2].z[1][0][1]+" "+c[1][fh.nrows-2].z[1][1][1]+" "+c[1][fh.nrows-2].z[1][2][1]);
		System.out.println(c[1][fh.nrows-2].z[1][0][0]+" "+c[1][fh.nrows-2].z[1][1][0]+" "+c[1][fh.nrows-2].z[1][2][0]);


	}
}