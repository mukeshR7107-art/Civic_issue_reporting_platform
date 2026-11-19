import {useEffect,useRef} from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type {Report} from './types';
export default function MapView({reports=[],point,onPick,onSelect}:{reports?:Report[];point?:[number,number];onPick?:(lat:number,lng:number)=>void;onSelect?:(id:string)=>void}){
 const node=useRef<HTMLDivElement>(null),map=useRef<L.Map|null>(null),layer=useRef<L.LayerGroup|null>(null);
 const pickRef=useRef(onPick);pickRef.current=onPick;
 useEffect(()=>{if(!node.current)return;const m=L.map(node.current,{scrollWheelZoom:false}).setView(point||[23.363,85.327],14);map.current=m;L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',maxZoom:19}).addTo(m);layer.current=L.layerGroup().addTo(m);m.on('click',(e:L.LeafletMouseEvent)=>pickRef.current?.(e.latlng.lat,e.latlng.lng));const resize=new ResizeObserver(()=>m.invalidateSize());resize.observe(node.current);return()=>{resize.disconnect();m.remove();map.current=null}},[]);
 useEffect(()=>{const l=layer.current;l?.clearLayers();for(const r of reports){const marker=L.circleMarker([r.latitude,r.longitude],{radius:9,color:'#fff',weight:3,fillColor:r.status==='CLOSED'?'#34865c':r.priority==='HIGH'?'#d67b42':'#226652',fillOpacity:1});marker.bindTooltip(r.trackingNumber);marker.on('click',()=>onSelect?.(r.id));l?.addLayer(marker)}if(point){l?.addLayer(L.circleMarker(point,{radius:10,color:'#fff',weight:3,fillColor:'#226652',fillOpacity:1}));map.current?.panTo(point)}},[reports,point?.[0],point?.[1],onSelect]);
 return <div className="map" ref={node} aria-label={onPick?'Choose report location on the map; coordinates can also be entered below':'Map of your visible reports'}/>;
}
