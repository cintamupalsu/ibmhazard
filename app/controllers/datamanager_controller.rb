class DatamanagerController < ApplicationController
  
  def region
    @regions = Region.all
  end
  
  def seedregion
    varTxt=params[:fileTXT]
    fTxt=open(varTxt.path,"r:UTF-8")
    until fTxt.eof do
      dline=fTxt.gets.chomp.split('|')
      if dline[4]!=nil
        Region.create(lat:dline[0].to_f, lon:dline[1].to_f, x:dline[2].to_i, y:dline[3].to_i, name:dline[4])      
      end
    end
    fTxt.close()
  end
  
  def deleteallregion
    # Region.delete_all
    @message = "Its done"
  end
  
  def hazard
    @hazards = Hazard.all  
  end
  
  def seedhazard
    varTxt=params[:fileHazardSeed]
    fTxt=open(varTxt.path,"r:UTF-8")
    until fTxt.eof do
      dline=fTxt.gets.chomp.split('|')
      if dline[0]!=nil
        #Hazard.create(name:dline[0])      
      end
    end
    fTxt.close()
  end
  
  def deleteallhazard
    #Hazard.delete_all
    @message = "Hazard deleted"
  end

  def zone
    @zones = Zone.all  
  end
  
  def seedzone
    varTxt=params[:fileZoneSeed]
    fTxt=open(varTxt.path,"r:UTF-8")
    linecount = 0
    region_name = ""
    detail = false
    until fTxt.eof do
      if linecount==0
        dline=fTxt.gets.chomp.split('|')
        @region = Region.where('name = ?',dline[0])
        #@hazard = Hazard.where('name = ?',dline[1]) permanent
        if @region.count>0 #&& @hazard.count>0 permanent
          detail=true
        end
      end
      if linecount>0 && detail
        dline=fTxt.gets.chomp.split('|')
        if dline[1]!=nil
          #Zone.create(region_id: @region[0].id, place: dline[0], lat: dline[1].to_f, lon: dline[2].to_f, x: dline[3].to_i, y: dline[4].to_i)      
        end
      end
      linecount+=1
    end
    fTxt.close()
  end
  
  def deleteallzone
    #Zone.delete_all
    @message = "Zone deleted"
  end

  def anchor
    @anchors = Anchor.all  
  end
  
  def seedanchor
    varTxt=params[:fileAnchorSeed]
    fTxt=open(varTxt.path,"r:UTF-8")

    until fTxt.eof do
      dline=fTxt.gets.chomp.split(';')
      if dline[1]!=nil
        #Anchor.create(adj_lat: dline[0].to_f, adj_lon: dline[1].to_f, cellsize: dline[2].to_f, name: dline[3], llat: dline[4].to_f, llon: dline[5].to_f, nrows: dline[6].to_i, ncols: dline[7].to_i, x: dline[8].to_i, y: dline[9].to_i)      
      end
    end
    fTxt.close()
  end
  
  def deleteallanchor
    #Anchor.delete_all
    @message = "Anchor deleted"
  end

  def rectangle
    @rectangles = Rectangle.all  
  end
  
  def seedrectangle
    varTxt=params[:fileRectangleSeed]
    fTxt=open(varTxt.path,"r:UTF-8")
    linecount = 0
    iteration = 0
    detail = false
    until fTxt.eof do
      if linecount==0
        dline=fTxt.gets.chomp.split(';')
        @anchor = Anchor.where('name = ?',dline[1])
        @hazard = Hazard.where('name = ?',dline[0]) 
        iteration = dline[2].to_i
        if @anchor.count>0 && @hazard.count>0 
          detail=true
        end
      end
      if linecount>0 && detail
        dline=fTxt.gets.chomp.split(';')
        if dline[1]!=nil
          @zone = Zone.where('place =?', dline[0])
          if@zone.count>0
            #Rectangle.create(anchor_id: @anchor[0].id, rank: dline[1], iteration: iteration.to_s, zone_id: @zone[0].id, hazard_id: @hazard[0].id, lat0: dline[2], lon0: dline[3], lat1: dline[4], lon1: dline[5], lat2: dline[6], lon2: dline[7], lat3: dline[8], lon3: dline[9], inside: dline[10])      
          end
        end
      end
      linecount+=1
    end
    fTxt.close()
  end
  
  def deleteallrectangle
    #Rectangle.delete_all
    @message = "Rectangle deleted"
  end


end
