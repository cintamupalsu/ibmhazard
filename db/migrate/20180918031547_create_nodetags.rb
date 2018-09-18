class CreateNodetags < ActiveRecord::Migration[5.1]
  def change
    create_table :nodetags do |t|
      t.string :k
      t.string :v
      t.references :node, foreign_key: true

      t.timestamps
    end
  end
end
