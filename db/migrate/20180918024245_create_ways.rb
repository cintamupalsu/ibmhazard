class CreateWays < ActiveRecord::Migration[5.1]
  def change
    create_table :ways do |t|
      t.string :osm
      t.references :zone, foreign_key: true

      t.timestamps
    end
  end
end
