require "./lib/assets/ruby/clsGeo.rb"
require "./lib/assets/ruby/clsGmap.rb"
class TsunamiController < ApplicationController
  def index
  end

  def map
    #sample parameters http://hazard-maulanamania.c9users.io/tsunami/map?coord=34.905062_138.347685_640_640_jpg_16_test_1
    #
    parastr = params[:coord]

    #breakdown parametes >>>
    dline= parastr.split('_')
    lat=dline[0].to_f
    lon=dline[1].to_f
    map_width=dline[2].to_i
    map_height=dline[3].to_i
    map_format=dline[4].to_s
    map_scale=dline[5].to_i
    map_type=dline[7].to_i
    username=dline[6]
    #breakdown parametes <<<
    
    # security reason
    if username.length<30 && map_format.length<4
      # generate map & panorama image >>>
      rectangle_map = gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username)
      panorama = gen_panorama(lat, lon, map_width, map_height, map_format, map_scale, map_type, username)
      # generate map & panorama image <<<
      render :json=>{map: rectangle_map, view: panorama}
    else
      render :json=>{map: "https://hazardmap.mybluemix.net/shared/images/paraerror.jpg", view: "http://hazard-maulanamania.c9users.io/picture/paraerror.jpg"}
    end
  end
  private
  def gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username)
    # declaration >>>
    clsGeo = ClsGeo.new()
    clsGmap = ClsGmap.new()
    # declaration <<<
    
    # select 2 closest zones >>>
    closest_zone = Array.new(2,0)
    closest_distance = -1 # default closest distance for null
    Zone.all.each do |zone|
      distance_to_zone = clsGeo.geo_distance(zone.lat, zone.lon, lat, lon, 0)
      # select 1st closest zone
      if closest_distance == -1
        closest_zone[0] = zone.id
        closest_distance = distance_to_zone
      end
      # select 2nd closest zone
      if distance_to_zone<closest_distance
        closest_zone[1] = closest_zone[0]
        closest_zone[0] = zone.id
        closest_distance = distance_to_zone
      end
    end
    # select 2 closest zones <<<
    
    # select 111 closest rectangle >>>
    top111 = 111
    adj_lat = -9999 
    adj_lon = -9999
    closest_rectangle = {}
    rectangles = Rectangle.where("zone_id IN (?) AND hazard_id = ?",closest_zone,1) # Hazard id 1 is Tsunami 10 meters hazard
    #(0..closest_zone.count-1).each do |i|
      #zone = Zone.find(closest_zone[i])
      rectangles.all.each do |rectangle|
      #zone.rectangles.all.each do |rectangle|
        distance_to_rectangle = -1 # default value for null in distance
        #if rectangle.hazard.id==1 # Hazard id 1 is Tsunami 10 meters hazard
          
          # get adjustment >>>
          if adj_lat==-9999 || adj_lon==-9999
            anchor = Anchor.find(rectangle.anchor_id)
            adj_lat = anchor.adj_lat
            adj_lon = anchor.adj_lon
          end
          # get adjustment <<<
          
          d1line_inside = rectangle.inside.split(',')
          (0..d1line_inside.count-1).each do |j|
            d2line_inside = d1line_inside[j].split(' ')
            cubic_lat = d2line_inside[0].to_f
            cubic_lon = d2line_inside[1].to_f
            distance_to_cubic = clsGeo.geo_distance(cubic_lat+adj_lat, cubic_lon+adj_lon, lat, lon, 0)
            if distance_to_rectangle==-1 || distance_to_rectangle > distance_to_cubic
              closest_rectangle[rectangle.id]=distance_to_cubic # get smallest
              distance_to_rectangle = distance_to_cubic
            end 
          end # end do j
        #end # end if
      end # end do rectangle
    #end # end do i
    closest_rectangle = closest_rectangle.sort_by(&:last)
    lat0 = Array.new(top111)
    lat1 = Array.new(top111)
    lat2 = Array.new(top111)
    lat3 = Array.new(top111)
    lon0 = Array.new(top111)
    lon1 = Array.new(top111)
    lon2 = Array.new(top111)
    lon3 = Array.new(top111)
    rectangle_value = Array.new(top111)
    counttop = 0
    
    closest_rectangle.each do |key, value|
      if counttop<top111
        rectangle = Rectangle.find(key)
        lat0[counttop] = rectangle.lat0
        lat1[counttop] = rectangle.lat1
        lat2[counttop] = rectangle.lat2
        lat3[counttop] = rectangle.lat3
        lon0[counttop] = rectangle.lon0
        lon1[counttop] = rectangle.lon1
        lon2[counttop] = rectangle.lon2
        lon3[counttop] = rectangle.lon3
        rectangle_value[counttop] = rectangle.rank
      else
        break
      end
      counttop+=1
    end
    # select 111 closest rectangle <<<
    
    # generate rectangles on map >>>
    gmapstatic_str_header = "https://maps.googleapis.com/maps/api/staticmap?"
    gmapstatic_str_params = "size=" + map_width.to_s + "x" + map_height.to_s + 
                            "&scale=1&center=" + lat.to_s + "," + lon.to_s +
                            "&zoom=" + map_scale.to_s
    gmapstatic_str_marker = "&markers=size:tiny%7Ccolor:yellow%7C" + lat.to_s + "," + lon.to_s
    if map_type==1
      gmapstatic_str_params+="&maptype=hybrid"
    end
    gmap_str_key = "&key="
    
    thickness = 33.to_s
    gmapstatic_str_rectangle = ""
    (0..top111-1).each do |i|
      color="000000"
      if rectangle_value[i]==3
        color="FF0000"
      end
      if rectangle_value[i]==2
        color="FFFF00"
      end
      if rectangle_value[i]==1
        color="00FF00"
      end
      header = "&path=fillcolor:0x"+color+thickness+"%7Ccolor:0xFFFFFF00%7Cenc:"
      body =[[lat0[i].to_f + adj_lat, lon0[i].to_f + adj_lon],
            [lat1[i].to_f + adj_lat, lon1[i].to_f + adj_lon],
            [lat2[i].to_f + adj_lat, lon2[i].to_f + adj_lon],
            [lat3[i].to_f + adj_lat, lon3[i].to_f + adj_lon]]
      ebody= clsGmap.myencode(body)
      if rectangle_value[i]!=nil
        gmapstatic_str_rectangle+=header+ebody
      end
    end #i
    gmapstatic_str = gmapstatic_str_header + 
                     gmapstatic_str_params +
                     gmapstatic_str_marker +
                     gmapstatic_str_rectangle +
                     gmap_str_key
            
    # generate rectangles on map <<<
    
    # registering user and map image >>>
    @guest = Guest.new
    @guest.lat=lat
    @guest.lon=lon
    @guest.identity=username
    
    fileRescue=true
    begin
      open("public/uploads/guest/picture/tsunami"+username+"."+map_format, 'wb') do |file|
        file << open(gmapstatic_str).read
        @guest.picture = file
        #file.delete
      end
    rescue
       fileRescue=false
    end
    # registering user and map image <<<
    
    # return link of map image >>>
    if @guest.save && fileRescue==true 
      staticmap = "https://hazardmap.mybluemix.net"+@guest.picture.url
      #staticmap = gmapstatic_str
    else
      staticmap = "https://hazardmap.mybluemix.net/shared/images/paraerror.jpg"
      #staticmap = gmapstatic_str
    end
    # return link of map image <<<
    
  end
  
  def gen_panorama(lat, lon, map_width, map_height, map_format, map_scale, map_type, username)
    # declaration >>>
    clsGeo = ClsGeo.new()
    clsGmap = ClsGmap.new()
    # declaration <<<
    
    # select 2 closest zones >>>
    closest_zone = Array.new(2,0)
    closest_distance = -1 # default closest distance for null
    Zone.all.each do |zone|
      distance_to_zone = clsGeo.geo_distance(zone.lat, zone.lon, lat, lon, 0)
      # select 1st closest zone
      if closest_distance == -1
        closest_zone[0] = zone.id
        closest_distance = distance_to_zone
      end
      # select 2nd closest zone
      if distance_to_zone<closest_distance
        closest_zone[1] = closest_zone[0]
        closest_zone[0] = zone.id
        closest_distance = distance_to_zone
      end
    end
    # select 2 closest zones <<<
    # find the closest node >>>

    nodes = Node.where("zone_id IN (?)",closest_zone) # Hazard id 1 is Tsunami 
           
    closest_distance_to_node=-1
    closest_node_id = 0
    next_node_id = 0
    nodes.all.each do |node|
      distance_to_node = clsGeo.geo_distance(lat,lon,node.lat,node.lon,0)
      if(closest_distance_to_node>distance_to_node || closest_distance_to_node == -1)
        next_node_id = closest_node_id
        closest_node_id = node.id
      end # end if
    end # end of nodes do  
    if closest_node_id !=0
      node = Node.find(closest_node_id)
      next_node = Node.find(next_node_id)
      tetha = get_angle(lat,lon,node.lat,node.lon)
      # find the closest node <<<
      # google map apis string generator >>>
      gmapstatic_str_header = "https://maps.googleapis.com/maps/api/streetview?"
      gmapstatic_str_params = "size="+map_width.to_s+"x"+map_height.to_s
      gmapstatic_str_position = "&location="+lat.to_s+","+lon.to_s+
                                "&heading="+tetha.to_s+
                                "&pitch=15&&fov=65"
      gmap_str_key = "&key="
      
      # google map apis string generator <<<
      # registering user and map image >>>
      @guest = Guest.new
      @guest.lat=lat
      @guest.lon=lon
      @guest.identity=username
      
      gmapstatic_str = gmapstatic_str_header + gmapstatic_str_params + gmapstatic_str_position + gmap_str_key
      
      fileRescue=true
      begin
        open("public/uploads/guest/picture/panorama"+username+"."+map_format, 'wb') do |file|
          file << open(gmapstatic_str).read
          @guest.picture = file
        end
      rescue
         fileRescue=false
      end
      # registering user and map image <<<
      
      # return link of map image >>>
      if @guest.save && fileRescue==true 
        staticmap = "https://hazardmap.mybluemix.net"+@guest.picture.url
        #staticmap = gmapstatic_str
      else
      
        staticmap = "https://hazardmap.mybluemix.net/shared/images/paraerror.jpg"
        #staticmap = gmapstatic_str
      end
    else
      staticmap = "https://hazardmap.mybluemix.net/shared/images/dataerror.jpg"
    end
    # return link of map image <<<
  end
  
  def get_angle(lat1, lon1, lat2, lon2)
    diflat = lat2-lat1
    diflon = lon2-lon1
    abslat = diflat.abs
    abslon = diflon.abs
    #Quadrant I
    if diflat>=0 && diflon>=0
      tetha= 90 - Math.asin(diflat/Math.sqrt(diflon*diflon+diflat*diflat)) * 57.295779513
      return tetha
    end
    #Quadrant II
    if diflat<0 && diflon>=0
      tetha= 180 - Math.asin(diflon/Math.sqrt(diflon*diflon+abslat*abslat)) * 57.295779513 
      return tetha
    end
    #Quadrant III
    if diflat<0 && diflon<0
      tetha= Math.asin(abslon/Math.sqrt(abslon*abslon+abslat*abslat)) * 57.295779513 + 180
      return tetha
    end
    #Quadrant IV
    if diflat>=0 && diflon<0
      tetha= Math.asin(diflat/Math.sqrt(abslon*abslon+diflat*diflat)) * 57.295779513 + 270
      return tetha
    end
    
  end
end
