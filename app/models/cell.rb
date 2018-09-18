class Cell < ApplicationRecord
  belongs_to :anchor
  belongs_to :hazard
end
