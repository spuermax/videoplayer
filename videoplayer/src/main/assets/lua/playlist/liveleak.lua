--[[
 $Id$

 Copyright © 2012 VideoLAN and AUTHORS

 Authors: Ludovic Fauvet <etix@videolan.org>

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
--]]

-- Probe function.
function probe()
    return vlc.access == "http"
        and string.match( vlc.path, "www.liveleak.com/view" )
end

-- Util function
function find( haystack, needle )
    local _,_,r = string.find( haystack, needle )
    return r
end

-- Parse function.
function parse()
    local p = {}
    local role
    local art
    local video

    while true do
        line = vlc.readline()
        if not line then break end

        -- Try to find the role
        if string.match( line, '<span class="section_title"' ) then
            role = find( line, '<span class="section_title"[^>]*>(.-)<' )
            role = string.gsub( role, '&nbsp;', ' ' )
        end

        -- Try to find the art
        if string.match( line, 'image:' ) then
            art = find( line, 'image: "(.-)"' )
        end

        -- Try to find the video
        if string.match( line, 'file:' ) then
            video = find( line, 'file: "(.-)"' )
        end
    end
    if video then
        table.insert( p, { path = video; name = role; arturl = art; } )
    end
    return p
end

