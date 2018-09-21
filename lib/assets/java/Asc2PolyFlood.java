package bin;
import java.io.*;

// Args:
// 0: file Asc
// 1: file ZoneList (id name lat lon)
// 2 Disaster Type
// 3 period (i.e. 60 = 60 min, 120 =120 min ...)
// 4 Middle Point 0- Highest lowest 1 - average
// 5 Anchor ID
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
		return dist;
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

class ZoneList{
	double lat, lon;
	String name;
	ZoneList(){
		lat=0;
		lon=0;
		name="";
	}
}

class CubicZone{
	int x, y, zoneID, stage;
	double[] lat, lon; 
	double value;
	CubicZone(AscHeader fh, int x, int y){
		lat= new double[4];
		lon= new double[4];				
		this.x = x;
		this.y = y;
		value=0;
		stage=0;
		
	}
}
class Polygons{
	int zoneID, level;
	double[] lat, lon;
	CubicZone[] cubics;
	Polygons(CubicZone cubic,int level) {
		cubics = new CubicZone[1];
		cubics[0] = cubic;
		lat = new double[4];
		lon = new double[4];
		for(int m=0; m<4; m++) {
			lat[m]=cubic.lat[m];
			lon[m]=cubic.lon[m];
		}
		this.level=level;
		zoneID=cubic.zoneID;
	}
	void PolyAddCubicH(CubicZone cubic){
		cubics = (CubicZone[])resizeArray(cubics, cubics.length+1);
		cubics[cubics.length-1]=cubic;	
		lat[2] = cubic.lat[2];
		lon[2] = cubic.lon[2];
		lat[3] = cubic.lat[3];
		lon[3] = cubic.lon[3];
	}
	
	private static Object resizeArray (Object oldArray, int newSize) {
		int oldSize = java.lang.reflect.Array.getLength(oldArray);
		Class elementType = oldArray.getClass().getComponentType();
		Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
		int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0) System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
		return newArray; 
	}
}

class Asc2PolyFlood{
	public static void main(String args[]) {
		AscHeader fh = new AscHeader(args[0]);
		Geodesic geo = new Geodesic();
		//Double[] stage = new Double[3];
		//stage[0]= Double.parseDouble(args[2]);
		//stage[1]= Double.parseDouble(args[3]);
		double hiVal =0;
		double avgVal =0;
		double cValNotZero=0;
		Polygons[] poly = new Polygons[0];
		// get ZoneList
		ZoneList[] zl = new ZoneList[0];
		try (BufferedReader br = new BufferedReader(new FileReader(args[1]))){
			String line;
		    while ((line = br.readLine()) != null) {
		    	zl = (ZoneList[])resizeArray(zl, zl.length+1);
		    	zl[zl.length-1]= new ZoneList();
		        String[] d1Line = line.split(" ");
		        zl[zl.length-1].lat=Double.parseDouble(d1Line[1]);
		        zl[zl.length-1].lon=Double.parseDouble(d1Line[2]);
		        zl[zl.length-1].name=d1Line[3];
		    }
		}catch(IOException e) {
			System.out.println("I/O error"+e);
		}
		// get ZoneList
		
		CubicZone[][] cubic = new CubicZone[fh.ncols][fh.nrows];
		
		// read and assign cubical >>>
		try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
			// Skip header >>>
			for(int i=0; i<6; i++) {
				String skipHeader = br.readLine();
			}
			// Skip header <<<
			for(int j=fh.nrows-1; j>=0; j--) {					
				String[] d1Line = br.readLine().split(" ");				
				for(int i=0; i<fh.ncols;i++) {
					cubic[i][j]= new CubicZone(fh,i,j);
					cubic[i][j].lat[0]= fh.llat+(fh.cellsize*j);
					cubic[i][j].lat[1]= cubic[i][j].lat[0]+ fh.cellsize; 
					cubic[i][j].lat[2]= cubic[i][j].lat[1];
					cubic[i][j].lat[3]= cubic[i][j].lat[0];
					
					cubic[i][j].lon[0]= fh.llon+(fh.cellsize*i);
					cubic[i][j].lon[1]= cubic[i][j].lon[0];
					cubic[i][j].lon[2]= cubic[i][j].lon[1]+ fh.cellsize;
					cubic[i][j].lon[3]= cubic[i][j].lon[2];							
					cubic[i][j].value = Double.parseDouble(d1Line[i]);
					if (cubic[i][j].value>hiVal) {
						hiVal=cubic[i][j].value;
					}
					if (cubic[i][j].value>0) {
						cValNotZero++;
					}
					avgVal+=cubic[i][j].value;
					int zlID=0;
					double closestDistance=6371000;
					for (int m=0; m< zl.length; m++) {
						double distance=geo.distance(zl[m].lat, zl[m].lon, cubic[i][j].lat[0]+(0.5*fh.cellsize), cubic[i][j].lon[0]+(0.5*fh.cellsize));
						if (closestDistance>distance){
							zlID=m;
							closestDistance = distance;
						}
					}
					cubic[i][j].zoneID= zlID;
					//System.out.print(zlID+" ");
				}
				//System.out.println();
			}			
		}catch(IOException e) {
			System.out.println("I/O error"+e);
		}
		//avgVal=avgVal/(fh.nrows*fh.ncols);
		//System.out.println(avgVal+" "+cValNotZero);
		//avgVal=avgVal/cValNotZero;
		//System.out.println(avgVal);
		int avgMode=Integer.parseInt(args[4]);
		if (avgMode==0) {
			avgVal=hiVal/2;
		}else {
			avgVal=avgVal/cValNotZero;
			hiVal=avgVal*2;
		}
		
		System.out.println(avgVal);
			
		double[] stg = new double[3];
		stg[0] = hiVal/3;
		stg[1] = hiVal/3*2;
		stg[2] = hiVal;

		System.out.println(stg[0]+" "+stg[1]);
		// read and assign cubical <<<
		for(int i=0;i<fh.ncols;i++) {
			for(int j=0;j<fh.nrows;j++) {
				if(cubic[i][j].value<=1 && cubic[i][j].value>0) {
					cubic[i][j].stage=0;
				}
				if(cubic[i][j].value<=1.5 && cubic[i][j].value>1) {
					cubic[i][j].stage=1;
				}
				if(cubic[i][j].value<=3 && cubic[i][j].value>1.5) {
					cubic[i][j].stage=2;
				}
				if(cubic[i][j].value>3) {
					cubic[i][j].stage=3;
				}								
			}
		}
		int prevStage=0;
		
		for(int j=0;j<fh.nrows;j++) {
			prevStage=0;
			for(int i=0; i<fh.ncols;i++) {
				if(cubic[i][j].stage>0) {
					if (cubic[i][j].stage!=prevStage) {
						poly=(Polygons[])resizeArray(poly, poly.length+1);
						poly[poly.length-1]= new Polygons(cubic[i][j], cubic[i][j].stage);						
					}else {
						poly[poly.length-1].PolyAddCubicH(cubic[i][j]);
					}					
				}								
				prevStage = cubic[i][j].stage;
			}
		}

		try (BufferedWriter bw = new BufferedWriter(new FileWriter("./result/seeds.rb"))){
			for(int m=0; m<zl.length;m++) {
				//String line = "Geozone.create!(lat:"+zl[m].lat+", lon:"+zl[m].lon+", place:'"+zl[m].name+"')\n";
				//bw.write(line);
			}
			for(int m=0; m<poly.length; m++) {
				int zone_id=poly[m].zoneID+1;
				String line_search = "zoneid = Zone.where('place = ?','"+zl[zone_id-1].name+"')\n";
				bw.write(line_search);
				String line = "Rectangle.create(hazard_id:"+args[2]+",anchor_id:"+args[5]+",iteration:"+args[3]+", zone_id: zoneid[0].id, rank:"+poly[m].level;
				for(int m1=0; m1<4; m1++) {
					line+= ", lat"+m1+":"+poly[m].lat[m1]+", lon"+m1+":"+poly[m].lon[m1];
				}
				line +=", inside:'";
				for(int m2=0; m2<poly[m].cubics.length; m2++) {
					double clat=poly[m].cubics[m2].lat[0]+(fh.cellsize/2);
					double clon=poly[m].cubics[m2].lon[0]+(fh.cellsize/2);
					line+=clat+" "+clon;
					if (m2!=poly[m].cubics.length-1) {
						line+=",";
					}
				}
				line += "')\n";
				bw.write(line);
			}
			bw.close();
		}catch(IOException e) {
			
		}
	}
	private static Object resizeArray (Object oldArray, int newSize) {
		int oldSize = java.lang.reflect.Array.getLength(oldArray);
		Class elementType = oldArray.getClass().getComponentType();
		Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
		int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0) System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
		return newArray; 
	}
}
	
