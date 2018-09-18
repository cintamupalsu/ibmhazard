class Way < ApplicationRecord
  belongs_to :zone
  has_many :wayrefs, dependent: :destroy
  has_many :waytags, dependent: :destroy
end
