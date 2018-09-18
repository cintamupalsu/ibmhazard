class Region < ApplicationRecord
  has_many :zones, dependent: :destroy
end
