const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../src/main/resources/assets/limesbackpacks/models/item');
const touching=(a,b)=>a.from.every((v,i)=>v<=b.to[i]+.001&&a.to[i]+.001>=b.from[i]);
const base=JSON.parse(fs.readFileSync(path.join(root,'backpack_hiking.json'))).elements.find(e=>e.name==='main_body');
for(const [rank,tier] of ['leather','copper','iron','gold','diamond','netherite'].entries()){
 const es=JSON.parse(fs.readFileSync(path.join(root,tier+'_backpack.json'))).elements;
 const body=es.find(e=>e.name==='main_body');
 const depth=(.86+rank*.065)*[.90,.88,.86,.83,.80,.77][rank]*.95;
 const close=(a,b)=>Math.abs(a-b)<.002;
 assert(close(body.to[2]-body.from[2],(base.to[2]-base.from[2])*depth),'Slimmer tier depth');
 assert(close(body.to[0]-body.from[0],(base.to[0]-base.from[0])*(.90+rank*.055)),'Preserve body width');
 assert(close(body.to[1]-body.from[1],(base.to[1]-base.from[1])*(.88+rank*.035)*1.05),'Square fabric body retains the 5% height increase');
 assert(!es.some(e=>['main_body_shoulder','main_body_crown','front_pocket_rounded_top'].includes(e.name)),'No stepped body or pocket caps');
 assert(close(body.to[2],12.25+(base.to[2]-12.25)*depth),'Back surface remains anchored');
 if(rank>=3){const roll=es.find(e=>e.name==='sleeping_bag_core');assert(close(roll.to[2]-roll.from[2],3.75),'Keep full bedroll depth');}
 console.log(tier+': PASS 5% taller body, unchanged width/depth and full-size bedroll');
}
for(const tier of ['iron','gold','diamond','netherite']) {
 const m=JSON.parse(fs.readFileSync(path.join(root,tier+'_backpack.json'))),es=m.elements;
 const body=es.find(e=>e.name==='flask_body');assert(body.to[2]-body.from[2]>3.2);assert(body.to[0]-body.from[0]>1.55&&body.to[0]-body.from[0]<1.65,'Fuller canteen thickness');
 const mount=es.find(e=>e.name==='flask_mount');
 assert(touching(body,mount),'Turned canteen must meet mount');
 assert(touching(mount,es.find(e=>e.name==='main_body')),'Mount must meet backpack body');
 for(const texture of Object.values(m.textures))assert(texture.startsWith('limesbackpacks:item/'),'Item model must use one item atlas');
 for(const e of es.filter(e=>/^flask_(body|shoulders|top|round_base)$/.test(e.name)))assert(Object.values(e.faces).every(f=>f.texture==='#cloth'&&f.uv[1]>10),'Canteen must use the brown leather swatches');
 const get=name=>es.find(e=>e.name===name);
 const panel=get('main_body');
 const centeredZ=(panel.from[2]+panel.to[2])/2;
 assert(Math.abs((body.from[2]+body.to[2])/2-centeredZ)<.002,'Canteen centered on the main rectangle, excluding the outer pocket');
 assert(body.to[2]<12.25,'Canteen clears the player-facing harness plane');
 assert(touching(get('canteen_carrier_face'),get('canteen_carrier_under')),'Carrier strap wraps under canteen');
 assert(touching(get('canteen_carrier_under'),get('canteen_carrier_return')),'Carrier return closes the cradle');
 assert(touching(get('canteen_carrier_return'),get('main_body')),'Canteen cradle connects to body');
 assert(touching(get('canteen_upper_loop'),get('flask_hanger'))&&touching(get('canteen_upper_loop'),get('flask_shoulders')),'Upper loop connects cover to hanger');
 assert(touching(get('flask_hanger'),get('flask_shoulders')),'Upper hanger meets cover');
 assert(touching(get('flask_hanger'),get('main_body')),'Upper hanger meets body');
 assert(!es.some(e=>e.name.startsWith('side_pocket_right')),'No pocket behind canteen');
 assert(touching(get('flask_waist'),body)&&touching(get('flask_waist'),get('flask_shoulders')),'Canteen covers are connected');
 assert(!get('lid_map_pocket'),'Remove unwanted upper pocket');
 if(tier!=='iron') {
  assert(Math.abs(get('sleeping_bag_bottom').from[1]-get('upper_front_panel').to[1]-.25)<.002,'Bedroll rests a quarter pixel above outer pocket');
  assert(Math.abs(get('sleeping_bag_core').to[1]-get('sleeping_bag_core').from[1]-2.375)<.002,'Lowered roll keeps its full height');
  assert(get('sleeping_bag_bottom').from[1]<get('main_body').to[1],'Bedroll tucks into lid');
  assert(get('sleeping_bag_core').to[1]>get('main_body').to[1],'Bedroll remains visible above lid');
  assert(touching(get('sleeping_bag_bottom'),get('main_body')),'Bedroll is supported');
  const expected={gold:[.64,1.28,7.36,14.72],diamond:[.64,1.28,15.36,7.36],netherite:[.32,11.093,3.68,15.573]};
  assert.deepEqual(get('sleeping_bag_core').faces.north.uv,expected[tier],'Distinct forest green / oatmeal beige / red rolls');
  if(tier==='diamond'){
   assert.equal(m.textures.bedroll,'limesbackpacks:item/sleeping_bag_oatmeal');
   for(const e of es.filter(e=>e.name==='sleeping_bag_bottom'||e.name.startsWith('bedroll_end_cloth_'))){
    for(const f of Object.values(e.faces)){assert.equal(f.texture,'#bedroll');assert.deepEqual(f.uv,[8.64,4.64,15.36,7.36]);}
   }
  }
 }
 if(tier==='netherite') {
  assert(!es.some(e=>e.name.startsWith('side_pocket_left')),'No pocket behind quiver');
  assert(get('quiver_base').from[0]<get('main_body').from[0],'Quiver on opposite side from canteen');
  assert(touching(get('quiver_right'),get('main_body')),'Quiver sits against pack without a gap');
  assert.equal(get('quiver_base').from[1],get('main_body').from[1],'Quiver base aligns with pack base');
  assert(get('quiver_base').to[0]-get('quiver_base').from[0]>=2.5,'Larger quiver');
  assert(get('arrow_shaft_0').to[1]-get('quiver_front').to[1]>=4,'Longer exposed arrows');
  for(const q of es.filter(e=>e.name.startsWith('quiver_')&&!e.name.startsWith('quiver_mount_')))for(const roll of es.filter(e=>/^(sleeping_bag_|bedroll_)/.test(e.name)))assert(!touching(q,roll),'Quiver clears bedroll');
  for(const arrow of es.filter(e=>e.name.startsWith('arrow_')))for(const roll of es.filter(e=>/^(sleeping_bag_|bedroll_)/.test(e.name)))assert(!touching(arrow,roll),'Arrows clear bedroll');
  for(const bracket of es.filter(e=>e.name.startsWith('quiver_mount_')))assert(touching(bracket,get('main_body'))&&touching(bracket,get('quiver_right')),'Quiver brackets attach both ends');
 }
 for(const anchor of es.filter(e=>e.name.startsWith('bedroll_tie_anchor_'))) {
  const x=anchor.name.slice('bedroll_tie_anchor_'.length);
  const loop=Array.from({length:12},(_,i)=>es.find(e=>e.name===`bedroll_tie_loop_${x}_${i}`));
  loop.forEach((e,i)=>assert(touching(e,loop[(i+1)%12]),`${tier} strap gap at ${i}`));
  assert(loop.some(e=>touching(e,anchor)),'Loop must meet anchor');
  assert(touching(anchor,es.find(e=>e.name==='main_body')),'Anchor must meet pack');
  const clasp=es.find(e=>e.name==='bedroll_tie_clasp_'+x);assert(loop.some(e=>touching(e,clasp)),'Clasp must meet strap');
 }
 const lantern=es.filter(e=>e.name.startsWith('vanilla_lantern_'));
 if(['diamond','netherite'].includes(tier)) {
  assert(touching(get('lantern_leather_root'),get('front_pocket_back'))&&touching(get('lantern_leather_root'),get('lantern_mount')),'Sewn lantern root bridges hanger to front pocket');
  assert(Math.abs(get('lantern_leather_root').from[2]-(get('lantern_mount').from[2]-.2))<.002,'Root sits directly behind lantern handle');
  const intersects=(a,b)=>a.from.every((v,i)=>v<b.to[i]-.001&&a.to[i]>b.from[i]+.001);
  for(const l of lantern.slice(0,2))for(const p of es.filter(e=>/^(main_body|upper_front_panel|lower_front_panel|front_pocket_back|side_pocket_left)/.test(e.name)))assert(!intersects(l,p),`${tier}: lantern must clear ${p.name}`);
  assert.equal(lantern.length,4);assert.equal(m.textures.lantern,'limesbackpacks:item/vanilla_lantern');
  const template=require('./vanilla-lantern-template.json');
  lantern.forEach((e,i)=>{for(const [side,f]of Object.entries(e.faces)){assert.equal(f.texture,'#lantern');assert.deepEqual(f.uv,template.elements[i].faces[side].uv);assert(!f.cullface);}});
 }
 console.log(tier+': PASS leather canteen, closed strap loops/anchors/buckles, vanilla lantern UVs');
}
for(const tier of ['copper','iron','gold','diamond','netherite']){
 const es=JSON.parse(fs.readFileSync(path.join(root,tier+'_backpack.json'))).elements;
 assert(!es.some(e=>e.name==='flask_cover_tab'),'No decorative upper flask tab');
 assert(!es.some(e=>e.name.startsWith('copper_pocket_corner_')),'No copper side-pocket buttons');
 if(tier==='netherite'){
  const bars=es.filter(e=>e.name.startsWith('netherite_flap_corner_')&&!e.name.includes('_rise_')).sort((a,b)=>a.from[0]-b.from[0]);
  assert.equal(bars.length,2);
  bars.forEach((bar,i)=>{
   const rise=es.find(e=>e.name===bar.name.replace('corner_','corner_rise_'));
   assert(rise&&touching(bar,rise),'Corner upright joins its horizontal bar');
   assert(Math.abs(i===0 ? rise.from[0]-bar.from[0] : rise.to[0]-bar.to[0])<.002,'Corner uprights mirror at outer edges');
  });
 }
 const fittings=es.filter(e=>/^(reinforced_foot_|bedroll_tie_clasp_|canteen_buckle|quiver_keeper_|copper_pocket_corner_|netherite_flap_corner_)/.test(e.name)||e.name==='lid_reinforcement');
 for(const e of fittings)for(const f of Object.values(e.faces)){
  assert.equal(f.texture,'#armor');
  assert(f.uv[0]>=5&&f.uv[2]<=7&&f.uv[1]>=11.5&&f.uv[3]<=13.5,'Metal samples opaque armor chest');
 }
 for(const name of ['upper_front_trim','upper_front_buckle','side_pocket_left_leather_band','side_pocket_left_leather_tab']){
  const e=es.find(e=>e.name===name);if(!e)continue;
  const face=e.faces[name.startsWith('side_')?'west':'north'];
  assert.equal(face.texture,'#armor');
  assert.deepEqual(face.uv,tier==='netherite'?[5.5,12,6.5,13]:[5,12,6,13],'Fastening uses four by two chunky armor texels');
 }
 console.log(tier+': PASS armor metal on fittings and pocket fastenings');
}
