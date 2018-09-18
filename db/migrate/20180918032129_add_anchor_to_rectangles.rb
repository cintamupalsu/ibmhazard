class AddAnchorToRectangles < ActiveRecord::Migration[5.1]
  def change
    add_reference :rectangles, :anchor, foreign_key: true
  end
end
