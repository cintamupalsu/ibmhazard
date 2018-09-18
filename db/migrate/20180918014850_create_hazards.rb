class CreateHazards < ActiveRecord::Migration[5.1]
  def change
    create_table :hazards do |t|
      t.string :name

      t.timestamps
    end
  end
end
