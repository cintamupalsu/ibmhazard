class CreateCells < ActiveRecord::Migration[5.1]
  def change
    create_table :cells do |t|
      t.references :anchor, foreign_key: true
      t.references :hazard, foreign_key: true
      t.float :lat
      t.float :lon
      t.integer :x
      t.integer :y
      t.float :v

      t.timestamps
    end
  end
end
