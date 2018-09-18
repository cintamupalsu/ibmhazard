class CreateWaytags < ActiveRecord::Migration[5.1]
  def change
    create_table :waytags do |t|
      t.string :model
      t.string :k
      t.string :v
      t.references :way, foreign_key: true

      t.timestamps
    end
  end
end
