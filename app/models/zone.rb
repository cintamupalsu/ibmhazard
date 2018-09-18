class Zone < ApplicationRecord
  belongs_to :region
  has_many :ways, dependent: :destroy
  has_many :nodes, dependent: :destroy
  has_many :rectangles, dependent: :destroy
end
