class CreateRectangles < ActiveRecord::Migration[5.1]
  def change
    create_table :rectangles do |t|
      t.integer :rank
      t.float :lat0
      t.float :lat1
      t.float :lat2
      t.float :lat3
      t.float :lon0
      t.float :lon1
      t.float :lon2
      t.float :lon3
      t.text :inside
      t.integer :iteration
      t.references :hazard, foreign_key: true
      t.references :zone, foreign_key: true

      t.timestamps
    end
  end
end
