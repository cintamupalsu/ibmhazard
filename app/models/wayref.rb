class Wayref < ApplicationRecord
  belongs_to :way
  belongs_to :node
end
