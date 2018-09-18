class DatamanagerController < ApplicationController
  
  def region
  
  end
  
  def seedregion
    varTxt=params[:fileTXT]
    fTxt=open(varTxt.path,"r:UTF-8")
    until fTxt.eof do
      dline=fTxt.gets.chomp.split(' ')
    end
    fTxt.close()
  end
  
  def deleteallregion
    Region.delete_all
    @message = "Its done"
  end
end
