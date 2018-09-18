class Anchor < ApplicationRecord
    has_many :rectangles, dependent: :destroy
    has_many :cells, dependent: :destroy
end
