require "./lib/assets/ruby/clsGeo.rb"
require "./lib/assets/ruby/clsGmap.rb"
class LandslideController < ApplicationController
  def index
  end
    
  def map
    #sample parameters http://ibmhazard-maulanamania.c9users.io/landslide/map?coord=34.905062_138.347685_640_640_jpg_16_test_1
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
      # generate map >>>
      rectangle_map = gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username)
    
      # generate map <<<
      render :json=>{map: rectangle_map}
    else
      render :json=>{map: "http://hazard-maulanamania.c9users.io/picture/paraerror.jpg", view: "http://hazard-maulanamania.c9users.io/picture/paraerror.jpg"}
    end
  end
  
  def gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username)
    # declaration >>>
    clsGeo = ClsGeo.new()
    clsGmap = ClsGmap.new()
    # declaration <<<

    # select 2 closest region >>>
    region_hash = {}
    Region.all.each do |region|
      distance_to_region = clsGeo.geo_distance(region.lat, region.lon, lat, lon, 0)
      region_hash[region.id] = distance_to_region
    end
    
    region_hash = region_hash.sort_by(&:last)
    closest_region = Array.new(2,0)
    region_counter=0
    region_hash.each do |key, value|
      if region_counter<closest_region.count
        closest_region[region_counter]=key.to_i
      else
        break
      end
      region_counter+=1
    end
    # select 2 closest region <<<

    # select 2 closest zones >>>
    zone_hash = {}
    closest_zone = Array.new(9,0)
    zones = Zone.where("region_id IN(?)",closest_region)
    zones.each do |zone|
      distance_to_zone = clsGeo.geo_distance(zone.lat, zone.lon, lat, lon, 0)
      zone_hash[zone.id] = distance_to_zone
    end
    zone_hash = zone_hash.sort_by(&:last)
    zone_counter=0
    zone_hash.each do |key, value|
      if zone_counter<closest_zone.count
        closest_zone[zone_counter]=key.to_i
      else
        break
      end
      zone_counter+=1
    end
    #closest_distance = -1 # default closest distance for null
    
    #zones = Zone.where("region_id IN(?)",closest_region)
    
    #Zone.all.each do |zone|
    #zones.each do |zone|
    #  distance_to_zone = clsGeo.geo_distance(zone.lat, zone.lon, lat, lon, 0)
    #  # select 1st closest zone
    #  if closest_distance == -1
    #    closest_zone[0] = zone.id
    #    closest_distance = distance_to_zone
    #  end
    #  # select 2nd, 3rd, 4th closest zone
    #  if distance_to_zone<closest_distance
    #    closest_zone[3] = closest_zone[2]
    #    closest_zone[2] = closest_zone[1]
    #    closest_zone[1] = closest_zone[0]
    #    closest_zone[0] = zone.id
    #    closest_distance = distance_to_zone
    #  end
    #end
    # select 2 closest zones <<<
    
    # select 111 closest rectangle >>>
    top111 = 111
    adj_lat = -9999 
    adj_lon = -9999
    closest_rectangle = {}
    rectangles = Rectangle.where("zone_id IN (?) AND hazard_id = ?",closest_zone,3) # Hazard id 1 is Tsunami 10 meters hazard
    rectangles.all.each do |rectangle|
      distance_to_rectangle = -1 # default value for null in distance
  
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
    end # end do rectangle
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
    gmapstatic_str_key = "&key="
    
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
                     gmapstatic_str_key
    # generate rectangles on map <<<
    
    # registering user and map image >>>
    @guest = Guest.new
    @guest.lat=lat
    @guest.lon=lon
    @guest.identity=username
    
    fileRescue=true
    begin
      open("public/uploads/guest/picture/landslide"+username+"."+map_format, 'wb') do |file|
        file << open(gmapstatic_str).read
        #file << open("http://ibmhazard-maulanamania.c9users.io/shared/images/QRCode.png").read
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
      staticmap = "https://hazardmap.mybluemix.net/picture/paraerror.jpg"
      #staticmap = gmapstatic_str
    end
    # return link of map image <<<
    
  end
  
end