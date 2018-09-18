class CreateZones < ActiveRecord::Migration[5.1]
  def change
    create_table :zones do |t|
      t.references :region, foreign_key: true
      t.string :place
      t.float :lat
      t.float :lon
      t.integer :x
      t.integer :y

      t.timestamps
    end
  end
end
