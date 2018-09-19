class ClsGmap
    
    def initialize()
        
    end

    def myencode(points)
      last_lat = last_lng = 0
      result = ""

      points.each do |point|
        ll = point
        lat = (ll[0] * 1e5).round.to_i
        lng = (ll[1] * 1e5).round.to_i
        d_lat = lat - last_lat
        d_lng = lng - last_lng

        [d_lat, d_lng].each do |v|
          v = (v < 0) ? ~(v << 1) : (v << 1)
          while v >= 0x20
            result += ((0x20 | (v & 0x1f)) + 63).chr
            v >>= 5
          end
          result += (v + 63).chr
        end

        last_lat = lat
        last_lng = lng
      end

      result
    end
  
    
    def convertion(coordinate)
        coord = (coordinate*100000).to_i
        #@coord = convertToBinary(@coord)
        if(coordinate>0)
            coord = 31.downto(0).map { |n| (coord<<1)[n] }.join
        else
            coord = 31.downto(0).map { |n| (~(coord<<1))[n] }.join
        end
        coordArray= chunkBinary(coord,5)        
        coordSlice= orArray(coordArray)
        coordDecimal=convertToDecimal(coordSlice)
        
        for i in 0..coordDecimal.count-1
            coordDecimal[i]+=63
        end
        
        coordASCII = convertToASCII(coordDecimal)
        
        result=""
        for i in 0..coordASCII.count
            result+=coordASCII[i].to_s
        end
        
        return result
    end
    
    def convertToASCII(coordDecimal)
       result={}
       for i in 0..coordDecimal.count-1
          result[i]=coordDecimal[i].chr
       end
       return result
    end
    
    def convertToDecimal(coordSlice)
        result={}
        for i in 0..coordSlice.count-1
            result[i]=coordSlice[i].to_i(2)
        end
        return result
    end
    
    def orArray(coordArray)
        slice5=coordArray
        #check how many part?
        allpart=5
        reduce=true
        for i in 0..5
            rev=5-i
            nozero=false
            if(reduce)
                for j in 0..4
                    if(slice5[rev][j]!="0")
                        nozero=true
                    end
                end
                if (nozero)
                    reduce=false
                else
                    allpart-=1
                end
            end
        end
        
        #process rest of part
        for i in 0..allpart-1
            slice5[i]="1"+slice5[i]
        end
        slice5[allpart]="0"+slice5[allpart]
        result={}
        for i in 0..allpart
            result[i] = slice5[i]
        end
        return result
    end
    
    def chunkBinary(binary,partialLength)
        result={}

        #chunk & reverse
        array=0
        bsliced = binary.slice(2,31)
        
        slice=""
        for i in 0..29
            if(i%partialLength==0)
                
                if (i!=0)
                    result[5-array]=slice
                    array+=1
                end
                slice=""
            end
            slice+=bsliced[i]
        end
        result[5-array]=slice
        
        return result
    end

end