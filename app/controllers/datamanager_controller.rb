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
        #Region.create(lat:dline[0].to_f, lon:dline[1].to_f, x:dline[2].to_i, y:dline[3].to_i, name:dline[4])      
      end
    end
    fTxt.close()
  end
  
  def deleteallregion
    # Region.delete_all
    @message = "Its done"
  end
end
