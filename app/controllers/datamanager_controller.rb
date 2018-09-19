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

end
