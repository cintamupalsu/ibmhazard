package bin;
import java.io.*;
// 1. ASC file
// 2. ASC result (in series) 
// 3. Tsunami High (in meters)
// 4. Tsunami Length (in meters)
class Manning10S {
	public static void main (String args[]) {
		double tHigh, tLength;
		double n = 0.014;
		AscCell[][] c;
		Direction d= new Direction();
		AscHeader fh = new AscHeader(args[0]);
		String fResult = args[1];
		fh.t.start();
		tHigh   = Double.parseDouble(args[2]);
		tLength = Double.parseDouble(args[3]);
		try{
			fh.t.join();
		}catch(InterruptedException e) {
			System.out.println("I/O Error: "+e);
		}
		c = new AscCell[fh.ncols][];
		for(int i=0; i<fh.ncols; i++) {
			c[i] = new AscCell[fh.nrows];
			for(int j=0; j<fh.nrows; j++) {
				c[i][j]= new AscCell(n);
			}
		}

		
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
			System.out.println(" Completed");
			// Init Surface <<<
									
		}catch(IOException e) {
			System.out.println("I/O Error: "+e);
		}	
		// iteration begin >>>
		boolean notDone=true;
		int[][] z_compare = new int[2][];
		z_compare[0]= new int[fh.ncols* fh.nrows];
		z_compare[1]= new int[fh.ncols* fh.nrows];
		int sec = 0;
		int endTsunami = (int)Math.round(tLength/fh.cellsize);
		int iCPU = Runtime.getRuntime().availableProcessors();
		do {
			// add tsunami from beach >>>
			if(sec<=endTsunami) {
				for(int i=0; i<fh.ncols; i++) {
					for(int j=0; j<fh.nrows; j++) {
						if(c[i][j].Z[1][1]==fh.noData) {
							boolean beach = false;
							for(int k=0; k<8; k++) {
								if(c[i][j].Z[1+d.i[k]][1+d.j[k]]==fh.noData) {
									beach= true;
								}
							} // end k
							if (beach) {
								c[i][j].z[0][1][1]=tHigh;
							}
						} // end if
					} // end j
				}	// end i
			}// end if
			// add tsunami from beach <<<
			
			// update z0
			for(int i=0; i<fh.ncols; i++) {
				for(int j=0; j<fh.nrows; j++) {
					for(int k=0; k<8; k++) {
						if((i+d.i[k]>=0) && (i+d.i[k]<fh.ncols) && (j+d.j[k]>=0) && (j+d.j[k]<fh.nrows)) {
							c[i][j].z[0][1+d.i[k]][1+d.j[k]]=c[i+d.i[k]][j+d.j[k]].z[0][1][1];
						}else {
							c[i][j].z[0][1+d.i[k]][1+d.j[k]]=c[i][j].z[0][1][1];
						}
					}
				}
			}
			// update z0

			int checkCell= 8;
			System.out.println("Before Manning and  z0 updated - Upper");			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][2]));
			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][1]));

			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][0]));
			System.out.println("Lower ----------------------------------------------------------------------------------------");
			checkCell++;
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][2]));
			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][1]));

			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][0]));
			System.out.println("----------------------------------------------------------------------------------------");

			// Manning Calculation >>>
			int usedCPU = 0;
			ManningCalculation[] mc = new ManningCalculation[iCPU];
			for(int i=0; i<fh.cr.length; i++) {
				mc[usedCPU] = new ManningCalculation(c[fh.cr[i].x][fh.cr[i].y],1,fh.noData,i);
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
			//AscWrite aw = new AscWrite(c,fResult, fh, sec);
			//aw.t.start();
			
			try {
				for(int j=0; j<usedCPU; j++) {
					mc[j].t.join();
				}
			}catch(InterruptedException e) {
				System.out.println("Main thread interrupted");
			}
			usedCPU=0;
			// Manning calculation >>>
			
			
			// comparing water expansion
			for(int i=0; i<fh.ncols; i++) {
				for(int j=0; j<fh.nrows; j++) {
					for(int k=0; k<2; k++) {
						if (c[i][j].z[k][1][1]>0) {
							z_compare[k][j*fh.ncols+i]=1;
						}else {
							z_compare[k][j*fh.ncols+i]=0;
						}
					} //next k
				} // next j
			} // next i
			notDone=false;
			for( int k=0; k<fh.ncols* fh.nrows;k++) {
				if(z_compare[0][k]!=z_compare[1][k]) {
					//System.out.println(z_compare[0][k]+" "+z_compare[1][k]);
					notDone=true;
				}
			}
			// end of comparing water expansion (if equal, simulation end)
			sec++;
			if (sec==2) notDone=false;
			
			checkCell= 8;
			System.out.println("After Manning Calculation - Upper");			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][2]));
			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][1]));

			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][0]));
			System.out.println("Lower----------------------------------------------------------------------------------------");
			checkCell++;
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][2]));
			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][1]));

			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][0]));

			System.out.println("----------------------------------------------------------------------------------------");

			for(int i=0; i<fh.ncols; i++) {
				for(int j=0; j<fh.nrows; j++) {
					c[i][j].z[0][1][1]=c[i][j].z[1][1][1];
				}
			}
			for(int i=0; i<fh.ncols; i++) {
				for(int j=0; j<fh.nrows; j++) {
					for(int k=0; k<8; k++) {
						if((i+d.i[k]>=0) && (i+d.i[k]<fh.ncols) && (j+d.j[k]>=0) && (j+d.j[k]<fh.nrows)) {
							c[i+d.i[k]][j+d.j[k]].z[0][1][1]+=c[i][j].z[1][1+d.i[k]][1+d.j[k]];
							c[i][j].z[1][1+d.i[k]][1+d.j[k]]=0;
						}
					}
				}
			}
			
			checkCell= 8;
			System.out.println("After Manning and  z0=z1");			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][2]));
			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][1]));

			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][0]));
			System.out.println("Lower----------------------------------------------------------------------------------------");
			checkCell++;
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][2])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][2])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][2])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][2]));
			
			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][1])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][1])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][1])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][1]));

			System.out.println(String.format("%.3f",c[2][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[0][2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].z[1][2][0])+"   "
					+String.format("%.3f",c[2][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[2][fh.nrows-checkCell].Z[2][0])+"---"
					+String.format("%.3f",c[3][fh.nrows-checkCell].Z[0][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[1][0])+" "+String.format("%.3f",c[3][fh.nrows-checkCell].Z[2][0]));

			System.out.println();
			System.out.println();

		}while(notDone);
		// iteration end <<<
		
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

class AscHeader implements Runnable{	
	String fName;
	int ncols, nrows; // number of columns and rows;
	double llat, llon, cellsize, noData;
	CellRank[] cr;
	Thread t;
	
	AscHeader(String fileName){
		fName=fileName;
		t = new Thread(this);
	}
	public void run() {
		String[] strHeader = new String[6];
		try(BufferedReader br = new BufferedReader(new FileReader(fName))){			
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
