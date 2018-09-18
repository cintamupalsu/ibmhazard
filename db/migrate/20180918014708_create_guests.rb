class CreateGuests < ActiveRecord::Migration[5.1]
  def change
    create_table :guests do |t|
      t.string :identity
      t.float :lat
      t.float :lon
      t.string :picture

      t.timestamps
    end
  end
end
