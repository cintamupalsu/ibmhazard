class Node < ApplicationRecord
  belongs_to :zone
  has_many :nodetags, dependent: :destroy
  has_many :wayrefs, dependent: :destroy
end
