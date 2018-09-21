package bin;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
// 0. File osm;
// 1. File region;
// 2. Series result (ftd, wtd, lbl, uid);
// 3. oneway mode 0 false 1 true
// 4. zone selection 0 all (1..~) selected
// 5. Word filter;

// ie: java bin.Osm2LabelList sup/osm/shizuoka.osm sup/zone.txt result/shizuoka 0 0
class Node{
	double lat;
	double lon;
	long id;
	Tag[] tag;
	float version;
	String userid;
	String username;
	boolean used;
	int gz; // geozone
	Node(){
		used=false;
		tag=new Tag[0];
	}
	void AddTag(Tag tg) {
		tag = (Tag[])resizeArray(tag, tag.length+1);
		tag[tag.length-1]=tg;
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
class Way{
	long[] n_id;
	long w_id;
	Tag[] tag;
	int zone;
	Way(){
		n_id = new long[0];
		tag = new Tag[0];
	}
	void AddNode(long node_id) {
		n_id = (long[])resizeArray(n_id, n_id.length+1);
		n_id[n_id.length-1]= node_id;
		//System.out.println(node_id+":"+n_id.length);
	}
	void AddTag(Tag tg) {
		tag = (Tag[])resizeArray(tag, tag.length+1);
		tag[tag.length-1]=tg;
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
class Tag{
	String k,v;
	Tag(){
		
	}
}

class LabelList{
	int[] con_node;
	int[] wtd_id;
	LabelList(){
		con_node = new int[0];
		wtd_id = new int[0];
	}
	void AddConnection(int node_array_num , int way_array_num) {
		con_node = (int[])resizeArray(con_node, con_node.length);
		wtd_id = (int[])resizeArray(wtd_id, wtd_id.length);
		con_node[con_node.length-1]=node_array_num;
		wtd_id[wtd_id.length-1]= way_array_num;
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

class WriteFtd implements Runnable{
	Node[] nd; //nodes
	String fn; //filename series
	ZoneList[] zl; // zoneList
	Thread t;
	
	WriteFtd(Node[] nodes, String filename, ZoneList[] zonelist){
		nd=nodes;
		fn=filename;
		zl=zonelist;
		t = new Thread(this);
	}
	
	public void run() {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn+"_ftd.txt"), "UTF-8"))){
			for(int i=0; i< nd.length; i++) {
				bw.write("nodecheck = Node.where(\"osm = ?\", \""+nd[i].id+"\")\n");
				bw.write("if nodecheck.length==0\n");
				bw.write("  zones = Zone.where('place = ?','"+zl[nd[i].gz-1].name+"')\n");
				bw.write("  Node.create(osm:\""+nd[i].id+"\", lat:"+nd[i].lat+", lon:"+nd[i].lon+", zone_id: zones[0].id)\n");				
				bw.write("end\n");				
				if (nd[i].tag.length>0) bw.write("nodecheck = Node.where(\"osm = ?\", \""+nd[i].id+"\")\n");
				for(int j=0; j<nd[i].tag.length;j++) {
					bw.write("k=\""+nd[i].tag[j].k+"\"\n");
					bw.write("v=\""+nd[i].tag[j].v+"\"\n");					
					bw.write("nodetagcheck = Nodetag.where(\"k = ? AND v = ? AND node_id = ?\",k,v,nodecheck[0].id)\n");
					bw.write("if nodetagcheck.length==0\n");
					bw.write("  Nodetag.create(k: k, v: v, node_id: nodecheck[0].id)\n");
					bw.write("end\n");
				}
			}
			bw.close();
		}catch(IOException e) {
			System.out.println("I/O error: "+e);
		}
	}
}
class WriteWtd implements Runnable{
	Way[] wy;
	Node[] nd;
	HashMap<Long, Integer> hashID;
	boolean owm;
	String fn;
	int zs;
	Thread t;
	WriteWtd(HashMap<Long, Integer> nodeOsmID, Way[] way, Node[] node, String filename, boolean onewaymode, int zoneselection){
		hashID = new HashMap<Long, Integer>();
		hashID = nodeOsmID;
		wy = way;
		nd = node;
		fn = filename;
		owm = onewaymode; // one way mode
		zs = zoneselection;
		t = new Thread(this);
	}
	public void run() {
		//try(BufferedWriter bw = new BufferedWriter(new FileWriter(fn+"_wtd.txt"))){
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fn+"_wtd.txt"), "UTF-8"))){
			for(int i=0; i<wy.length; i++) {
				
				if (zs==0 || zs ==wy[i].zone) { 
					bw.write("retway= Way.where(\"osm = ?\", \""+wy[i].w_id+"\")\n");
					bw.write("if retway.length==0\n");
					bw.write("  Way.create!(osm: \""+wy[i].w_id+"\", geozone_id: "+wy[i].zone+" )\n");
					bw.write("end\n");					
					if(wy[i].tag.length>0 || wy[i].n_id.length>0) bw.write("retway= Way.where(\"osm = ?\", \""+wy[i].w_id+"\")\n");
					boolean oneway=false;
					for(int j=0; j< wy[i].tag.length; j++) {
						bw.write("k=\""+wy[i].tag[j].k+"\"\n");
						bw.write("v=\""+wy[i].tag[j].v+"\"\n");
						if (wy[i].tag[j].k.contentEquals("oneway") && wy[i].tag[j].v.contentEquals("yes")){
							oneway=true;
						}
						bw.write("waytagcheck = Waytag.where(\"k = ? AND v = ? AND way_id = ?\",k,v,retway[0].id)\n");
						bw.write("if waytagcheck.length==0\n");
						bw.write("  Waytag.create!(k: k, v: v, way_id: retway[0].id)\n");
						bw.write("end\n");
					}
					for(int j=0; j< wy[i].n_id.length; j++) {
						bw.write("retnode = Node.where(\"osm = ?\", \""+wy[i].n_id[j]+"\")\n");
						bw.write("wayrefcheck = Wayref.where(\"node_id = ? AND way_id = ?\", retnode[0].id, retway[0].id)\n");
						bw.write("if wayrefcheck.length==0\n");
						if (j<wy[i].n_id.length-1) {
							bw.write("  retnextnode = Node.where(\"osm = ?\", \""+wy[i].n_id[j+1]+"\")\n");						
							bw.write("  Wayref.create!(node_id: retnode[0].id, way_id: retway[0].id, nextnode: retnextnode, backway: 0 )\n");
						}
						if ( j>0 && (oneway==false || owm == false)) {
							bw.write("  retprevnode = Node.where(\"osm = ?\", \""+wy[i].n_id[j-1]+"\")\n");						
							bw.write("  Wayref.create!(node_id: retnode[0].id, way_id: retway[0].id, nextnode: retprevnode, backway: 1 )\n");						
						}
						bw.write("end\n");
					}
				}				
			}
			bw.close();
		}catch(IOException e) {
			
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
class Osm2LabelList {
	public static void main (String args[]) {
		// get ZoneList
		ZoneList[] zl = new ZoneList[0];
		Geodesic geo = new Geodesic();
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
		// read osm file for counting
		int nNodes =0;
		int nWays =0;
		HashMap<Long, Integer> nodeOsmID = new HashMap<Long, Integer>();
		try (BufferedReader br = new BufferedReader(new FileReader(args[0]))){
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replace("\t", "");
				
				String[] dline = line.split(" ");
				for(int i=0; i<dline.length; i++) {					
					if (dline[i].contentEquals("<node")) {
						nNodes++;
					}
					if (dline[i].contentEquals("<way")){
						nWays++;
					}
				}
			}			
			br.close();
		}catch(IOException e) {
			System.out.println("I/O Error : "+e);
		}
		// assign node
		System.out.println("Nodes: " + nNodes + ", Ways: "+nWays);
		Node[] nd = new Node[nNodes];
		Way[] wy = new Way[nWays];
		int cNode=0;
		int cWay=0;
		//new InputStreamReader(new FileInputStream("DirectionResponse.xml"), "UTF-8")
		//try (BufferedReader br = new BufferedReader(new FileReader(args[0]))){
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]),"UTF-8"))){
			String line;
			boolean nodeinside = false;
			boolean wayinside = false;
			while ((line = br.readLine())!=null) {
				
				line = line.replace("\t", " ");
				
				String[] dline = line.split(" ");
				for (int i=0; i<dline.length; i++) {
					if(dline[i].contentEquals("<node") && !nodeinside) {
						nodeinside=true;
						nd[cNode]= new Node();
						cNode++;
						break;
					}
					if(dline[i].contentEquals("<way") && !wayinside) {
						wayinside=true;
						wy[cWay]= new Way();
						cWay++;
						break;
					}
				}
				
				// -- node section
				if (nodeinside) {
					for (int i=0; i<dline.length; i++) {
						String[] d1line=dline[i].split("=");						
						if(d1line[0].contentEquals("id")) {							
							String[] d2line= d1line[1].split("\"");
							nd[cNode-1].id=Long.parseLong(d2line[1]);
							nodeOsmID.put(nd[cNode-1].id, cNode-1);
						}
						if(d1line[0].contentEquals("lat")) {		
							String[] d2line= d1line[1].split("\"");
							nd[cNode-1].lat=Double.parseDouble(d2line[1]);
						}
						if(d1line[0].contentEquals("lon")) {									
							String[] d2line= d1line[1].split("\"");				
							nd[cNode-1].lon=Double.parseDouble(d2line[1]);
						}
						if(dline[i].contentEquals("<tag")){							
							Tag tag = new Tag();
							for(int j=i+1; j<dline.length;j++) {
								String[] d1Aline= dline[j].split("=");
								if(d1Aline[0].contentEquals("k")) {
									String[] d2line= d1Aline[1].split("\"");
									tag.k=d2line[1];
								}
								if(d1Aline[0].contentEquals("v")) {
									//String[] d2line= d1line[1].split("\"");
									String[] d2line=line.split("\"");
									tag.v=d2line[3];
								}
							}// end for j
							nd[cNode-1].AddTag(tag);
						} // end if tag
					}					
				}
				int cutfrom = dline[dline.length-1].length()-2;
				String lastChunk = dline[dline.length-1].substring(cutfrom,cutfrom+2);
				if (nodeinside) {
					boolean begin_with_node=false;
					for(int i=0; i< dline.length; i++) {
						if (dline[i].contentEquals("</node>")){
							nodeinside=false;
							break;
						}
						if (dline[i].contentEquals("<node")){
							begin_with_node=true;
						}
						if (begin_with_node && lastChunk.contentEquals("/>")) {
							nodeinside=false;
							break;
						}						
					}					
				}
				// --- end node section
				// --- way section 
				if(wayinside) {
					boolean nd_mode=false;
					for(int i=0; i<dline.length; i++) {
						String[] dheader=dline[i].split("=");
						
						if(dheader[0].contentEquals("id")) {							
							String[] d1line= dheader[1].split("\"");
							wy[cWay-1].w_id=Long.parseLong(d1line[1]);
						}
						if(dline[i].contentEquals("<nd")) {
							String[] d1line= dline[i+1].split("=");
							for(int j=0; j<d1line.length; j++) {
								if(d1line[j].contentEquals("ref")) {
									String[] d2line = d1line[j+1].split("\"");
									long nodeIDinWay=Long.parseLong(d2line[1]);
									if(nodeOsmID.get(nodeIDinWay)!=null) {
										wy[cWay-1].AddNode(nodeIDinWay);
										nd[nodeOsmID.get(nodeIDinWay)].used=true;
									}
								} // end if
							} // end for j						
						} // end if	"<nd>"
						if(dline[i].contentEquals("<tag")){							
							Tag tag = new Tag();
							for(int j=i+1; j<dline.length;j++) {
								String[] d1line= dline[j].split("=");
								if(d1line[0].contentEquals("k")) {
									String[] d2line= d1line[1].split("\"");
									tag.k=d2line[1];
								}
								if(d1line[0].contentEquals("v")) {
									//String[] d2line= d1line[1].split("\"");
									String[] d2line=line.split("\"");
									tag.v=d2line[3];
								}
							}// end for j
							wy[cWay-1].AddTag(tag);
						} // end if tag
					} // end for i
				} // end if wayinside
				
				if(wayinside) {
					boolean begin_with_way=false;
					for(int i=0; i< dline.length; i++) {
						if (dline[i].contentEquals("</way>")){
							wayinside=false;
							break;
						}
						if (dline[i].contentEquals("<way")){
							begin_with_way=true;
						}
						if (begin_with_way && lastChunk.contentEquals("/>")) {
							wayinside=false;
							break;
						}
					}
				}
				//-- end way section				
			}
		}catch(IOException e) {
			System.out.println("I/O Error : "+ e);
		}
		// delete unused node
		int count_used_node=0;
		for(int i=0; i<nd.length;i++) {
			//System.out.println(nd[i].used+" "+i);
			if(nd[i].used) {
				count_used_node++;
			}
		}
		Node[] und = new Node[count_used_node];
		count_used_node=0;
		nodeOsmID = new HashMap<Long, Integer>();
		for(int i=0; i<nd.length;i++) {
			if(nd[i].used) {
				und[count_used_node]=nd[i];
				nodeOsmID.put(nd[i].id, count_used_node);
				count_used_node++;
			}			
		}
		nd=new Node[0];
		//for(int i=0; i<und.length;i++) {
			
			//System.out.println(i+"-"+und[i].id+"-"+und[i].used);
		//}
		// end delete unused node
		
		// define zone
		// node		
		for(int i=0; i<und.length; i++) {
			double closestdistance = -1;
			for(int j=0; j<zl.length; j++) {
				double distance = geo.distance(zl[j].lat, zl[j].lon,und[i].lat, und[i].lon);
				if (closestdistance<0 || closestdistance>distance) {
					und[i].gz = j+1;
					closestdistance=distance;
				}				
			}
		}
		// write ftd (parallel)
		System.out.println("Used nodes : " + count_used_node);
		WriteFtd wf = new WriteFtd(und, args[2],zl);
		wf.t.start();
		// end write ftd
		
		// way
		for(int i=0; i<wy.length; i++) {
			double tlat= 0;
			double tlon= 0;
			for( int j=0; j<wy[i].n_id.length; j++) {
				int nodeID= nodeOsmID.get(wy[i].n_id[0]);
				//System.out.println(nodeID);
				tlat+=und[nodeID].lat;
				tlon+=und[nodeID].lon;
			}
			tlat=(double)tlat/(double)wy[i].n_id.length;
			tlon=(double)tlon/(double)wy[i].n_id.length;
			//System.out.println(tlat+" "+tlon);
			int closestZone=-1;
			double closestDist=999999999;
			for(int j=0;j<zl.length;j++) {
				double distance = geo.distance(tlat,tlon,zl[j].lat,zl[j].lon);
				//System.out.println(distance);
				if (distance<closestDist) {
					closestZone=j+1;
					closestDist=distance;			
				}
			}
			wy[i].zone=closestZone;
			
			//System.out.println(closestZone);
		}		
		// end define zone
		
		
		// define lbl
		boolean onewaymode=false;
		if (Integer.parseInt(args[3])==1) {
			onewaymode=true;
		}
		int zoneselection = Integer.parseInt(args[4]);
		WriteWtd ww = new WriteWtd(nodeOsmID, wy, und, args[2], onewaymode, zoneselection);
		ww.t.start();
		// end define lbl
		
		
		try {
			wf.t.join();
			ww.t.join();
		}catch(InterruptedException e) {
			System.out.println("Main thread interrupted");
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
