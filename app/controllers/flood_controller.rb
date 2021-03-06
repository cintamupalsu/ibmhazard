require "./lib/assets/ruby/clsGeo.rb"
require "./lib/assets/ruby/clsGmap.rb"
class FloodController < ApplicationController
  def index
  end
    
  def map
    #sample parameters http://ibmhazard-maulanamania.c9users.io/flood/map?coord=34.783155_138.284074_640_640_jpg_16_test_1_60_120_180
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
    sim_time = Array.new(3,0)
    sim_time[0]=dline[8].to_i
    sim_time[1]=dline[9].to_i
    sim_time[2]=dline[10].to_i
    
    #breakdown parametes <<<
    
    # security reason
    if username.length<30 && map_format.length<4
      # generate map >>>
      #rectangle_map_1 = gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username, sim_time[0])
      #rectangle_map_2 = gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username, sim_time[1])
      #rectangle_map_3 = gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username, sim_time[2])
      rectangle_map = gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username, sim_time)
    
      # generate map <<<
      render :json=>{map_1: rectangle_map[0], map_2: rectangle_map[1], map_3: rectangle_map[2]}
      # render :json=>{map_2: rectangle_map_2, map_3: rectangle_map_3}
    else
      render :json=>{map_1: "https://hazardmap.mybluemix.net/shared/images/paraerror.jpg", map_2: "http://hazard-maulanamania.c9users.io/picture/paraerror.jpg", map_3: "http://hazard-maulanamania.c9users.io/picture/paraerror.jpg"}
    end
  end
  
  def gen_rectangle_map(lat, lon, map_width, map_height, map_format, map_scale, map_type, username, sim_time)
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
    closest_zone = Array.new(4,0)
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
    # select 2 closest zones <<<
    
    # select 111 closest rectangle >>>
    hazardRec= Hazard.where("name = ?", 'Flood 88mm/hour') #Achtung!
    top111 = 111
    adj_lat = -9999 
    adj_lon = -9999
    closest_rectangle1 = {}
    closest_rectangle2 = {}
    closest_rectangle3 = {}
    rectangles = Rectangle.where("zone_id IN (?) AND hazard_id = ? AND iteration IN (?)",closest_zone, hazardRec[0].id, sim_time) # corrected
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
          if rectangle.iteration==sim_time[0]
            closest_rectangle1[rectangle.id]=distance_to_cubic # get smallest
          end
          if rectangle.iteration==sim_time[1]
            closest_rectangle2[rectangle.id]=distance_to_cubic # get smallest
          end
          if rectangle.iteration==sim_time[2]
            closest_rectangle3[rectangle.id]=distance_to_cubic # get smallest
          end
          distance_to_rectangle = distance_to_cubic
        end 
      end # end do j
    end # end do rectangle
    closest_rectangle1 = closest_rectangle1.sort_by(&:last)
    closest_rectangle2 = closest_rectangle2.sort_by(&:last)
    closest_rectangle3 = closest_rectangle3.sort_by(&:last)
    staticmap = Array.new(3,"")
    
    # first image
    staticmap[0] = gen_flood_series_image(top111,closest_rectangle1,sim_time[0], map_width, map_height, lat, lon, map_scale, map_type, adj_lat, adj_lon, username, map_format)
    
    # third image
    staticmap[1] = gen_flood_series_image(top111,closest_rectangle2,sim_time[1], map_width, map_height, lat, lon, map_scale, map_type, adj_lat, adj_lon, username, map_format)
   
    # third image
    staticmap[2] = gen_flood_series_image(top111,closest_rectangle3,sim_time[2], map_width, map_height, lat, lon, map_scale, map_type, adj_lat, adj_lon, username, map_format)

    return staticmap
    # return link of map image <<<
    
  end
  
  private
  
  def gen_flood_series_image(top111, closest_rectangle, sim_time, map_width, map_height, lat, lon, map_scale, map_type, adj_lat, adj_lon, username, map_format)
    
    clsGmap = ClsGmap.new()

    
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
      if counttop<top111 || rectangle_value[counttop]==nil
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
    gmapstatic_str_key = "&key="
    if map_type==1
      gmapstatic_str_params+="&maptype=hybrid"
    end
    
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
                     gmapstatic_str_rectangle + gmapstatic_str_key
    # generate rectangles on map <<<
    
    # registering user and map image >>>
    @guest = Guest.new
    @guest.lat=lat
    @guest.lon=lon
    @guest.identity=username
    
    fileRescue=true
    begin
      open("public/uploads/guest/picture/flood"+sim_time.to_s+username+"."+map_format, 'wb') do |file|
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

    
    return staticmap
    # return link of map image <<<

  end
end