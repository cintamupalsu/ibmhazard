class Rectangle < ApplicationRecord
  belongs_to :hazard
  belongs_to :zone
  belongs_to :anchor
end
