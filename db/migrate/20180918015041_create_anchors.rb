class CreateAnchors < ActiveRecord::Migration[5.1]
  def change
    create_table :anchors do |t|
      t.float :adj_lat
      t.float :adj_lon
      t.float :cellsize
      t.string :name
      t.float :llat
      t.float :llon
      t.float :nrows
      t.float :ncols
      t.integer :x
      t.integer :y

      t.timestamps
    end
  end
end
