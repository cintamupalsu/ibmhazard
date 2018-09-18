class CreateWayrefs < ActiveRecord::Migration[5.1]
  def change
    create_table :wayrefs do |t|
      t.integer :n_node
      t.integer :p_node
      t.float :n_distance
      t.float :p_distance
      t.references :way, foreign_key: true
      t.references :node, foreign_key: true

      t.timestamps
    end
  end
end
