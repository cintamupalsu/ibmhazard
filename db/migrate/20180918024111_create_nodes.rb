class CreateNodes < ActiveRecord::Migration[5.1]
  def change
    create_table :nodes do |t|
      t.string :osm
      t.float :lat
      t.float :lon
      t.references :zone, foreign_key: true

      t.timestamps
    end
  end
end
