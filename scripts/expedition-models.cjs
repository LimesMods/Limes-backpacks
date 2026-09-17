// Deterministic model authoring. Prints a file map; apply it using apply_patch.
// Original Flok geometry/UVs are preserved for the harness and flap hardware.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const base = JSON.parse(fs.readFileSync(path.join(root, 'src/main/resources/assets/limesbackpacks/models/item/backpack_hiking.json')));
const tiers = ['leather', 'copper', 'iron', 'gold', 'diamond', 'netherite'];
const clone = v => structuredClone(v);
const round = n => Math.round(n * 1000) / 1000;
const directions = ['north','south','east','west','up','down'];
const files = {};
const wornModel = require('./worn-model.cjs');
const lanternTemplate = JSON.parse(fs.readFileSync(path.join(__dirname,'vanilla-lantern-template.json')));
// Fit the projected silhouette into a 16-pixel GUI slot with a small margin.
// Measure the actual attachments as well as the body, then center the result.
const fitGui = model => {
  const rotate=(p,axis,degrees)=>{
    const angle=degrees*Math.PI/180,c=Math.cos(angle),s=Math.sin(angle);
    const [a,b]=axis===0?[1,2]:axis===1?[2,0]:[0,1];
    const q=[...p];q[a]=p[a]*c-p[b]*s;q[b]=p[a]*s+p[b]*c;return q;
  };
  const points=[];
  for(const e of model.elements)for(const x of [e.from[0],e.to[0]])for(const y of [e.from[1],e.to[1]])for(const z of [e.from[2],e.to[2]]){
    let p=[x,y,z];
    if(e.rotation){
      const r=e.rotation,axis={x:0,y:1,z:2}[r.axis];
      p=rotate(p.map((v,i)=>v-r.origin[i]),axis,r.angle);
      if(r.rescale)p=p.map((v,i)=>i===axis?v:v/Math.cos(r.angle*Math.PI/180));
      p=p.map((v,i)=>v+r.origin[i]);
    }
    points.push(rotate(rotate(p.map(v=>v-8),1,215),0,25));
  }
  const lo=[0,1].map(i=>Math.min(...points.map(p=>p[i])));
  const hi=[0,1].map(i=>Math.max(...points.map(p=>p[i])));
  const scale=round(Math.min(13.5/(hi[0]-lo[0]),13.5/(hi[1]-lo[1])));
  model.display.gui={rotation:[25,215,0],translation:[round(-(lo[0]+hi[0])*scale/2),round(-(lo[1]+hi[1])*scale/2),0],scale:[scale,scale,scale]};
};
// Item-frame transforms use the model's unrotated pixel space. The mesh is
// intentionally anchored below the model origin for the wearable renderer,
// so explicitly recenter it in a frame instead of relying on the default
// origin. The enlarged fixed scale is only for item frames.
const fitFixed = model => {
  const points=[];
  for(const e of model.elements) {
    points.push(e.from, e.to);
  }
  const lo=[0,1].map(i=>Math.min(...points.map(p=>p[i])));
  const hi=[0,1].map(i=>Math.max(...points.map(p=>p[i])));
  const center=[(lo[0]+hi[0])/2,(lo[1]+hi[1])/2];
  const scale=1.2375; // vanilla fixed scale 0.75 increased by 65%
  model.display.fixed={
    rotation:[0,0,0],
    translation:[round(8-center[0]),round(8-center[1]),0],
    scale:[scale,scale,scale]
  };
};

// In third person the backpack should sit in the hand instead of hovering
// above it, and its front should face away from the player like the held item.
const fitThirdPersonHand = model => {
  const transform={rotation:[0,180,0],translation:[0,0,0],scale:[.55,.55,.55]};
  model.display.thirdperson_righthand=clone(transform);
  model.display.thirdperson_lefthand=clone(transform);
};
for (const [rank, tier] of tiers.entries()) {
  const elements = [];
  const width = .90 + rank * .055, height = .88 + rank * .035;
  const originalDepth = .86 + rank * .065;
  // Progressively slimmer canvas: 10 / 12 / 14 / 17 / 20 / 23 percent less
  // depth. Keep width, height and the player-facing surface unchanged.
  // A further gentle 5% reduction from the slimmer profile on every tier.
  const depth = originalDepth * [.90,.88,.86,.83,.80,.77][rank] * .95;
  // The player-facing surface and top remain anchored as capacity grows outward/downward.
  const transform = p => [round(8+(p[0]-8)*width), round(16+(p[1]-16)*height), round(12.25+(p[2]-12.25)*depth)];
  const swatch = index => { const col=index%4,row=Math.floor(index/4); return [(col+.08)*4,(row+.08)*16/3,(col+.92)*4,(row+.92)*16/3].map(round); };
  const cloth = swatch(rank);
  // Sample the quiet highlight/shadow patches already present in the cloth atlas.
  // This keeps small fittings and sewn edges in the same muted material family.
  const patch=(index,light=true)=>{
    const u=(index%4+(light?.76:.16))*4;
    const v=(Math.floor(index/4)+(light?.18:.78))*16/3;
    return [u,v,u+.12,v+.12].map(round);
  };
  const materials = {
    cloth:['#cloth',cloth], strap:['#cloth',swatch(11)],
    metal:rank>=1 ? ['#armor',[5,11.5,7,13.5]] : ['#cloth',patch(rank)],
    edge:['#cloth',patch(rank)],
    fold:['#cloth',patch(rank,false)],
    thread:['#cloth',patch(9)],
    hardware:rank>=1 ? ['#armor',[5,11.5,7,13.5]] : ['#cloth',patch(2,false)],
    darkHardware:['#armor',[5,12,5.25,13.5]],
    dark:['#legacy',[8,1,11,4]],
    // Opaque near-black texel from the netherite atlas for the recessed interior.
    quiverShadow:['#equipment',[10.55,3.05,10.95,3.45]],
    roll: rank===4 ? ['#bedroll',[.64,1.28,15.36,7.36]] : rank===3 ? ['#bedroll',[.64,1.28,7.36,14.72]] : ['#cloth',swatch(rank+3)],
    rollLining:['#bedroll',[8.64,4.64,15.36,7.36]],
    flame:['#equipment',[12.25,6.5,13.75,9.5]],
    // Light neutral white from the iron atlas, independent of the pack's tier.
    feather:['#iron',[1,5.5,3,6.5]],
    wood:['#leather',[3,1,5,4]],
    hide:['#cloth',swatch(9)],
    hideDark:['#cloth',swatch(10)],
    hideSeam:['#cloth',swatch(11)],
  };
  const add = (name, from, to, material='cloth', overrides={}) => {
    const [texture,uv] = materials[material];
    elements.push({name,from:from.map(round),to:to.map(round),faces:Object.fromEntries(directions.map(d=>[d,{texture,uv:clone(uv),...overrides[d]}]))});
  };
  // Retain the familiar hiking front pocket and two wraparound compression straps.
  for (const name of ['main_body','left_outer_strap','right_outer_strap','upper_front_panel','upper_front_trim','upper_front_buckle','lower_front_panel']) {
    const e = clone(base.elements.find(e=>e.name===name));
    e.from=transform(e.from); e.to=transform(e.to);
    if (['main_body','lower_front_panel','upper_front_panel'].includes(name)) {
      for (const face of Object.values(e.faces)) { face.texture='#cloth'; face.uv=clone(cloth); }
    }
    elements.push(e);
  }
  // Keep the flap's top flush with the pocket behind it, removing the raised lip.
  elements.find(e=>e.name==='upper_front_panel').to[1]=elements.find(e=>e.name==='lower_front_panel').to[1];
  const left=transform([4,6,7])[0], right=transform([12,16,12])[0];
  const bottom=transform([4,6,7])[1], front=transform([4,6,7])[2];
  const pocketFront=transform([4.25,7.85,4.701])[2];
  // Stitch-like edging and hardware stand proud of the canvas, not coplanar with it.
  add('front_pocket_seam',[left+.7,bottom+1.8,pocketFront-.10],[right-.7,bottom+2.02,pocketFront+.05],'strap');
  const pocket = (name,x0,x1,y0,y1,z0,z1) => {
    add(name,[x0,y0,z0],[x1,y1,z1]);
    add(name+'_flap',[x0,y1-.8,z0],[x1,y1,z1],'cloth');
    // Match the main pocket's leather trim, with the fastening facing outward
    // on the left side (west), rather than toward the backpack's front (north).
    const leatherDetail=(suffix,from,to,sourceName)=>{
      add(name+suffix,from,to,'hide');
      const source=base.elements.find(e=>e.name===sourceName);
      const turn={north:'west',west:'south',south:'east',east:'north',up:'up',down:'down'};
      elements.at(-1).faces=Object.fromEntries(Object.entries(source.faces).map(([side,face])=>[turn[side],clone(face)]));
    };
    const middle=(z0+z1)/2;
    leatherDetail('_leather_band',[x0-.18,y1-1.35,z0+.25],[x0+.03,y1-.75,z1-.25],'upper_front_trim');
    leatherDetail('_leather_tab',[x0-.3,y1-1.55,middle-.4],[x0-.14,y1-.65,middle+.4],'upper_front_buckle');
  };
  if(rank>=1) {
    if(rank<5) pocket('side_pocket_left',left-1.6,left+.10,bottom+.5,bottom+4.6,front+.2,11.8);
    for(const x of [left+.1,right-.6]) add('reinforced_foot_'+x,[x,bottom-.15,front-.1],[x+.5,bottom+.45,12.3],'metal');
  }
  if(rank>=2) {
    // Leather field canteen: broad outward face, two covers and a recessed waist.
    // Center on the main rectangle, excluding the protruding front pocket.
    // A small move away from the arm from the previous midpoint position.
    const mainPanel=elements.find(e=>e.name==='main_body');
    const centeredZ=(mainPanel.from[2]+mainPanel.to[2])/2;
    const x=right-.15,z=centeredZ,y=bottom+1.2;
    add('flask_body',[x,y+.22,z-1.3],[x+1.1,y+1.85,z+1.3],'hideDark');
    add('flask_round_base',[x+.1,y,z-1.08],[x+1.0,y+.25,z+1.08],'hideDark');
    add('flask_waist',[x+.12,y+1.8,z-1.1],[x+.98,y+2.3,z+1.1],'hideSeam');
    add('flask_shoulders',[x,y+2.25,z-1.2],[x+1.1,y+3.5,z+1.2],'hide');
    add('flask_top',[x+.1,y+3.45,z-.95],[x+1,y+3.7,z+.95],'hide');
    add('flask_cork',[x+.3,y+3.65,z-.3],[x+.8,y+4.1,z+.3],'hideDark');
    add('flask_mount',[right-.25,y+.65,z-.25],[x+.16,y+1.25,z+.25],'hideSeam');
    add('flask_hanger',[right-.25,y+2.9,z-.25],[x+.16,y+3.4,z+.25],'hideSeam');
    // Uniform 15% enlargement, including the mounts, around the side attachment.
    const flaskPivot=[right,y+2.05,z];
    for(const e of elements.filter(e=>e.name.startsWith('flask_'))) {
      for(const p of [e.from,e.to])for(let axis=0;axis<3;axis++)p[axis]=round(flaskPivot[axis]+(p[axis]-flaskPivot[axis])*1.15);
    }
    // Fuller leather body: 25% thicker outward and 8% broader, same height.
    // Anchor the depth change at the pack wall; the cradle is fitted below from
    // these final bounds, so the buckle and wrapping straps follow the new body.
    for(const e of elements.filter(e=>e.name.startsWith('flask_'))){
      for(const p of [e.from,e.to]){
        p[0]=round(right+(p[0]-right)*1.25);
        p[2]=round(z+(p[2]-z)*1.08);
      }
    }
    // Carry handle with actual open space inside.
    if(rank===2) for(const x of [6.8,8.8]) add('handle_post_'+x,[x,15.9,9.3],[x+.4,16.8,9.8],'strap');
    if(rank===2) add('carry_handle',[6.8,16.65,9.3],[9.2,17.05,9.8],'strap');
  }
  if(rank>=3) {
    // Stepped octagonal bedroll resting across the lid. End rings expose rolled layers.
    // Keep the full round roll and its quiver clearance. The slimmer lid tucks
    // underneath it; its rear anchors still overlap the main compartment.
    const y=17.4,z=round(12.25+(7-12.25)*originalDepth)+1.1;
    add('sleeping_bag_core',[left-.9,y-.95,z-1.5],[right+.9,y+.95,z+1.5],'roll');
    add('sleeping_bag_top',[left-.9,y+.95,z-1],[right+.9,y+1.4,z+1],'roll');
    add('sleeping_bag_bottom',[left-.9,y-1.4,z-1],[right+.9,y-.95,z+1],'roll');
    for(const x of [left-.96,right+.9]) {
      add('bedroll_end_rim_'+x,[x,y-.75,z-1.1],[x+.06,y+.75,z+1.1],'strap');
      add('bedroll_end_cloth_'+x,[x-.02,y-.5,z-.8],[x+.09,y+.5,z+.8],'roll');
      add('bedroll_end_fold_'+x,[x-.04,y-.12,z-.45],[x+.11,y+.12,z+.45],'strap');
    }
    for(const x of [left+1.1,right-1.7]) {
      // Closed strap loop follows every corner of the actual stepped roll profile.
      const profile=[[1.4,-1],[.95,-1],[.95,-1.5],[-.95,-1.5],[-.95,-1],[-1.4,-1],[-1.4,1],[-.95,1],[-.95,1.5],[.95,1.5],[.95,1],[1.4,1]];
      for(let i=0;i<profile.length;i++) {
        const a=profile[i],b=profile[(i+1)%profile.length];
        add('bedroll_tie_loop_'+x+'_'+i,[x,y+Math.min(a[0],b[0])-.06,z+Math.min(a[1],b[1])-.06],[x+.6,y+Math.max(a[0],b[0])+.06,z+Math.max(a[1],b[1])+.06],'strap');
      }
      add('bedroll_tie_anchor_'+x,[x,15.8,z+1.44],[x+.6,y-.85,z+1.56],'strap');
      add('bedroll_tie_clasp_'+x,[x-.05,y-.2,z-1.68],[x+.65,y+.35,z-1.54],'metal');
    }
    // Thicken the complete roll assembly by 25%, including straps and anchors.
    // Sink the lower quarter into the lid and move inward slightly for a tucked fit.
    for(const e of elements.filter(e=>/^(sleeping_bag_|bedroll_)/.test(e.name))) {
      for(const p of [e.from,e.to]) {
        p[1]=round(16+(p[1]-16)*1.25-.95);
        p[2]=round(z+(p[2]-z)*1.25+.35);
      }
    }
    add('lid_reinforcement',[left+.3,15.85,front-.15],[right-.3,16.12,front+.55],'metal');
  }
  if(rank>=4) {
    // Lantern: amber core, four metal cage posts, stepped caps and a hanging loop.
    // Bring the lantern half a pixel back toward the pocket after the initial
    // one-pixel clearance adjustment. The arm ends follow the lantern center.
    const x=left-1.1,y=bottom+2.1,z=front-1.75;
    const lanternScale=.46;
    const lanternPoint=p=>[round(x+(p[0]-8)*lanternScale),round(y+p[1]*lanternScale),round(z+(p[2]-8)*lanternScale)];
    lanternTemplate.elements.forEach((source,i)=>{
      const e=clone(source);e.name='vanilla_lantern_'+i;e.from=lanternPoint(e.from);e.to=lanternPoint(e.to);
      if(e.rotation)e.rotation.origin=lanternPoint(e.rotation.origin);
      for(const face of Object.values(e.faces))delete face.cullface;
      elements.push(e);
    });
    // Connect sideways into the front pocket immediately behind the handle,
    // instead of running a long arm rearward to the main compartment.
    const pocketSide=transform([4.25,7.85,4.701])[0];
    add('lantern_mount',[x-.12,y+10*lanternScale,z-.1],[pocketSide+.10,y+11*lanternScale,z+.1],'hideSeam');
  }
  if(rank>=5) {
    // Full-size open leather quiver opposite the canteen.
    // Lower the quiver by 3 model units and nest its inner wall in the pack.
    // Place it toward the rear edge so the exposed shafts clear the bedroll.
    const x=left-1.1,z=10.0,y=bottom,top=14.5;
    add('quiver_base',[x-1.25,y,z-1.25],[x+1.25,y+.35,z+1.25],'hideDark');
    add('quiver_front',[x-1.25,y,z-1.25],[x+1.25,top,z-.95],'hideDark');
    add('quiver_back',[x-1.25,y,z+.95],[x+1.25,top,z+1.25],'hideDark');
    add('quiver_left',[x-1.25,y,z-.95],[x-.95,top,z+.95],'hideDark');
    add('quiver_right',[x+.95,y,z-.95],[x+1.25,top,z+.95],'hideDark');
    // Hide the shaft ends below a shadowed inset, keeping the mouth recessed.
    // Overlap the inner walls slightly so oblique views cannot reveal a gap.
    add('quiver_shadow_inset',[x-.97,top-.9,z-.97],[x+.97,top-.8,z+.97],'quiverShadow');
    for(const yy of [y+.7,top-.5]) {
      add('quiver_band_front_'+yy,[x-1.32,yy,z-1.32],[x+1.32,yy+.4,z-1.24],'hide');
      add('quiver_band_back_'+yy,[x-1.32,yy,z+1.24],[x+1.32,yy+.4,z+1.32],'hide');
      add('quiver_band_left_'+yy,[x-1.32,yy,z-1.32],[x-1.24,yy+.4,z+1.32],'hide');
      add('quiver_band_right_'+yy,[x+1.24,yy,z-1.32],[x+1.32,yy+.4,z+1.32],'hide');
    }
    for(const yy of [y+1.0,11.8]) {
      add('quiver_mount_'+yy,[x+1.1,yy,z-.4],[left+.1,yy+.4,z+.4],'strap');
    }
    for(const [i,dx,dz,h] of [[0,-.5,-.45,18.5],[1,.5,.15,19.2],[2,-.3,.5,18.9]]) {
      add('arrow_shaft_'+i,[x+dx-.13,top-1.4,z+dz-.13],[x+dx+.13,h,z+dz+.13],'wood');
      add('arrow_feather_x_'+i,[x+dx-.48,h-1.4,z+dz-.09],[x+dx+.48,h-.15,z+dz+.09],'feather');
      add('arrow_feather_z_'+i,[x+dx-.09,h-1.4,z+dz-.48],[x+dx+.09,h-.15,z+dz+.48],'feather');
    }
  }
  // Final tailoring pass. All equipment origins and item display transforms stay
  // as authored above; these details only change the construction of the pack.
  const get=name=>elements.find(e=>e.name===name);
  const recolor=(e,material)=>{
    const [texture,uv]=materials[material];
    for(const f of Object.values(e.faces)){f.texture=texture;f.uv=clone(uv);}
  };
  // Keep a continuous UV field when a cuboid is divided into tailored pieces.
  // Repeating the entire swatch on each shallow step would create dark stripes.
  const cropUvs=(part,source)=>{
    const axes={north:[0,1,false,true],south:[0,1,true,true],east:[2,1,false,true],west:[2,1,true,true],up:[0,2,false,true],down:[0,2,false,false]};
    for(const [side,f] of Object.entries(part.faces)){
      const [u,v,flipU,flipV]=axes[side],uv=source.faces[side].uv;
      const interval=(axis,flip)=>{
        const size=source.to[axis]-source.from[axis];
        let lo=Math.max(0,Math.min(1,(part.from[axis]-source.from[axis])/size));
        let hi=Math.max(0,Math.min(1,(part.to[axis]-source.from[axis])/size));
        return flip?[1-hi,1-lo]:[lo,hi];
      };
      const a=interval(u,flipU),b=interval(v,flipV);
      f.uv=[uv[0]+a[0]*(uv[2]-uv[0]),uv[1]+b[0]*(uv[3]-uv[1]),uv[0]+a[1]*(uv[2]-uv[0]),uv[1]+b[1]*(uv[3]-uv[1])].map(round);
    }
  };
  const originalBody=clone(get('main_body'));
  // Keep the body as a single square cuboid with its full original UV field.
  // The pocket also has a flat, full-width top without a stepped cap.
  // Remove overlap between the flap and shell while keeping their outer bounds.
  const flap=get('upper_front_panel'),pouch=get('lower_front_panel');
  const flapBounds=clone(flap),pouchBounds=clone(pouch);
  pouch.to[1]=flap.from[1];
  add('front_pocket_back',[pouch.from[0],flap.from[1],flap.to[2]],[pouch.to[0],flap.to[1],pouch.to[2]]);
  cropUvs(pouch,pouchBounds);
  cropUvs(get('front_pocket_back'),pouchBounds);
  cropUvs(flap,flapBounds);
  // Continuous, low-contrast piping follows the bottom and sides of the pocket.
  const fx=flapBounds.from[0],tx=flapBounds.to[0],fz=flapBounds.from[2];
  add('flap_sewn_edge',[fx+.22,flapBounds.to[1]-.45,fz-.035],[tx-.22,flapBounds.to[1]-.36,fz+.012],'edge');
  for(const xx of [pouch.from[0]+.12,pouch.to[0]-.20]) {
    add('pocket_side_seam_'+xx,[xx,pouch.from[1]+.18,pouch.from[2]-.032],[xx+.08,pouch.to[1]-.18,pouch.from[2]+.012],'edge');
  }
  recolor(get('front_pocket_seam'),'fold');
  if(get('side_pocket_left')) {
    const s=get('side_pocket_left'),sf=get('side_pocket_left_flap');
    // The former flap occupied the shell's volume. Trim the shell below it.
    s.to[1]=sf.from[1];
    for(const zz of [s.from[2]+.16,s.to[2]-.24])add('side_pocket_seam_'+zz,[sf.from[0]-.025,s.from[1]+.2,zz],[sf.from[0]+.012,s.to[1]-.13,zz+.08],'edge');
    add('side_flap_edge',[sf.from[0]-.028,sf.to[1]-.23,sf.from[2]+.18],[sf.from[0]+.01,sf.to[1]-.14,sf.to[2]-.18],'edge');
  }
  if(rank>=2) {
    const f=get('flask_body'),h=get('flask_hanger');
    const cradleBottom=get('flask_round_base').from[1];
    const zz=(f.from[2]+f.to[2])/2;
    // Leather carrier runs down the outward face and under the bottle, returning
    // to the existing pack mount. The upper loop bridges to the existing hanger.
    const bandX=f.to[0]+.045;
    add('canteen_carrier_face',[f.to[0]-.02,cradleBottom-.06,zz-.19],[bandX,f.to[1]+.06,zz+.19],'hideSeam');
    add('canteen_carrier_under',[right-.2,cradleBottom-.075,zz-.19],[bandX,cradleBottom+.03,zz+.19],'hideSeam');
    add('canteen_carrier_return',[right-.22,cradleBottom-.075,zz-.19],[right-.08,get('flask_mount').to[1],zz+.19],'hideSeam');
    const cover=get('flask_shoulders');
    add('canteen_upper_loop',[h.from[0],h.to[1]-.13,zz-.19],[cover.to[0]+.04,h.to[1]+.04,zz+.19],'hideSeam');
    // Buckle is an open rectangular frame, with leather visible through it.
    const buckle=(name,x,y,z,w,hgt,material='hardware')=>{
      add(name+'_top',[x,y+hgt-.09,z],[x+w,y+hgt,z+.08],material);
      add(name+'_bottom',[x,y,z],[x+w,y+.09,z+.08],material);
      for(const xx of [x,x+w-.09])add(name+'_side_'+xx,[xx,y+.09,z],[xx+.09,y+hgt-.09,z+.08],material);
    };
    // Canteen buckle faces east, so rotate a temporary north-facing frame.
    const start=elements.length;
    buckle('canteen_buckle',zz-.3,f.to[1]-.48,bandX,.6,.48);
    for(const e of elements.slice(start))for(const p of [e.from,e.to]){const old=p[0];p[0]=p[2];p[2]=old;}
    if(rank===2) {
      const handle=get('carry_handle');
      add('iron_padded_handle',[7.35,handle.from[1]-.07,handle.from[2]-.07],[8.65,handle.to[1]+.07,handle.to[2]+.07],'hideDark');
      for(const xx of [7.5,8.35])add('iron_handle_stitch_'+xx,[xx,handle.to[1]+.07,handle.from[2]-.075],[xx+.09,handle.to[1]+.085,handle.to[2]+.075],'thread');
    }
  }
  if(rank>=3) {
    const roll=get('sleeping_bag_core');
    // Diamond pairs an oatmeal outer roll with sand lining at the bottom and
    // exposed rolled ends. Keep its dark straps and metal clasps unchanged.
    if(rank===4){
      for(const e of elements.filter(e=>e.name==='sleeping_bag_bottom'||e.name.startsWith('bedroll_end_cloth_')))recolor(e,'rollLining');
      // Sample two columns and two rows of the existing beige palette rather
      // than four stacked bands. Turn the patch so its light area is upper
      // right and its shadow lower left, like the other fabric swatches.
      for(const name of ['sleeping_bag_core','sleeping_bag_top'])
        for(const face of Object.values(get(name).faces))face.rotation=180;
    }
    // Use nearby lighter/darker areas of the same roll swatch for sewn hems.
    const [rt,ruv]=materials.roll;
    const rw=ruv[2]-ruv[0],rh=ruv[3]-ruv[1];
    materials.rollEdge=[rt,[ruv[0]+rw*.67,ruv[1]+rh*.1,ruv[0]+rw*.74,ruv[1]+rh*.17].map(round)];
    materials.rollFold=[rt,[ruv[0]+rw*.12,ruv[1]+rh*.78,ruv[0]+rw*.19,ruv[1]+rh*.85].map(round)];
    if(rank===4){
      materials.rollEdge=['#bedroll',[2,5,2.12,5.12]];
      materials.rollFold=['#bedroll',[10,2,10.12,2.12]];
    }
    add('sleeping_bag_sewn_hem',[roll.from[0]+.14,roll.to[1]-.22,roll.from[2]-.026],[roll.to[0]-.14,roll.to[1]-.13,roll.from[2]+.014],'rollEdge');
    for(const [i,xx] of [roll.from[0]+.35,roll.to[0]-1.0].entries())add('sleeping_bag_soft_fold_'+i,[xx,roll.from[1]+.24,roll.from[2]-.02],[xx+.65,roll.from[1]+.32,roll.from[2]+.014],'rollFold');
  }
  if(rank>=4) {
    const mount=get('lantern_mount');
    const mountZ=(mount.from[2]+mount.to[2])/2;
    // Sewn patch on the pocket side at the green-marked handle position.
    add('lantern_leather_root',[fx-.09,mount.from[1]-.24,mountZ-.30],[fx+.10,mount.to[1]+.12,mountZ+.30],'hideDark');
    for(const yy of [mount.from[1]-.13,mount.to[1]-.02])add('lantern_root_stitch_'+yy,[fx-.11,yy,mountZ-.20],[fx-.085,yy+.075,mountZ+.20],'thread');
    // Diamond's double row of piping sits flush on the existing front flap.
    if(rank===4)add('diamond_double_piping',[fx+.22,flapBounds.to[1]-.66,fz-.032],[tx-.22,flapBounds.to[1]-.59,fz+.01],'edge');
  }
  if(rank===5) {
    // The base and walls share a boundary instead of overlapping faces.
    for(const name of ['quiver_front','quiver_back','quiver_left','quiver_right']){
      const wall=get(name),source=clone(wall);
      wall.from[1]=get('quiver_base').to[1];
      cropUvs(wall,source);
    }
    for(const mount of elements.filter(e=>e.name.startsWith('quiver_mount_'))) {
      const q=get('quiver_left'),zz=(q.from[2]+q.to[2])/2,yy=mount.from[1];
      // Upper and lower harnesses run from the mount around the leather tube.
      add('quiver_harness_outer_'+yy,[q.from[0]-.075,yy,q.from[2]-.36],[q.from[0]+.02,yy+.4,q.to[2]+.36],'strap');
      for(const z of [get('quiver_front').from[2]-.045,get('quiver_back').to[2]-.045])add('quiver_harness_return_'+yy+'_'+z,[q.from[0]-.075,yy,z],[left+.1,yy+.4,z+.09],'strap');
      // Tier-colored metal keeper on the outward leather harness.
      add('quiver_keeper_'+yy,[q.from[0]-.11,yy+.08,zz-.28],[q.from[0]-.07,yy+.32,zz+.28],'hardware');
    }
    for(const xx of [fx+.12,tx-.62]) {
      add('netherite_flap_corner_'+xx,[xx,flapBounds.from[1]+.1,fz-.045],[xx+.5,flapBounds.from[1]+.22,fz+.015],'edge');
      // Put each upright at the outer edge: mirrored L-shaped corner guards.
      const riseX=xx===fx+.12 ? xx : xx+.38;
      add('netherite_flap_corner_rise_'+xx,[riseX,flapBounds.from[1]+.1,fz-.045],[riseX+.12,flapBounds.from[1]+.63,fz+.015],'edge');
    }
  }
  // Pocket-opening bands and tabs use the same radiant tier metal as fittings.
  // Leather harnesses, cloth stitching and the vanilla lantern keep their colors.
  if(rank>=1){
    for(const e of elements.filter(e=>['upper_front_trim','upper_front_buckle','side_pocket_left_leather_band','side_pocket_left_leather_tab'].includes(e.name)||e.name.startsWith('netherite_flap_corner_'))){
      recolor(e,'metal');
    }
  }
  // Lower the complete bedroll assembly to sit just above the outer pocket.
  // Move the closed loops, hems and rear anchors with the cloth so no strap
  // separates from the roll. Keep the accepted body depth and equipment sizes.
  if(rank>=3){
    const pocketTop=get('upper_front_panel').to[1];
    const drop=pocketTop+.25-get('sleeping_bag_bottom').from[1];
    for(const e of elements.filter(e=>/^(sleeping_bag_|bedroll_)/.test(e.name))){
      for(const p of [e.from,e.to]){
        p[1]=round(p[1]+drop);
        // Clear the quiver's upper band at the new, lower netherite height.
        if(rank===5)p[2]=round(p[2]-.15);
      }
    }
  }
  if(rank>=3){
    const panel=get('main_body'),rollBottom=get('sleeping_bag_bottom').from[1];
    // A shallow strip of the canvas's own darker shade suggests contact shadow
    // beneath the roll. Its face sits just proud of the main fabric surface.
    add('bedroll_contact_shadow',
      [panel.from[0]+.3,rollBottom-.16,panel.from[2]-.018],
      [panel.to[0]-.3,rollBottom-.025,panel.from[2]+.008],'fold');
  }
  if(rank===5){
    const wall=get('quiver_left'),middle=(wall.from[2]+wall.to[2])/2;
    // One small dark-netherite plate on the outward wall, below the top band.
    add('quiver_metal_accent',
      [wall.from[0]-.035,wall.to[1]-.95,middle-.38],
      [wall.from[0]+.01,wall.to[1]-.58,middle+.38],'darkHardware');
  }
  if(rank>=4) {
    // Emissive window overlays only: keep the lantern's iron frame, cap, and
    // the rest of the backpack shaded normally. No texture changes required.
    const lamp=get('vanilla_lantern_0'),a=lamp.from,b=lamp.to,inset=.46,eps=.003;
    for(const side of ['north','south','west','east']) {
      const from=[a[0]+inset,a[1]+inset,a[2]+inset];
      const to=[b[0]-inset,b[1]-inset,b[2]-inset];
      const axis=side==='north'||side==='south'?2:0;
      from[axis]=to[axis]=(side==='north'||side==='west'?a[axis]-eps:b[axis]+eps);
      elements.push({name:'lantern_glow_'+side,from:from.map(round),to:to.map(round),
        shade:false,light_emission:15,faces:{[side]:{texture:'#lantern',uv:[1,3,5,8]}}});
    }
  }
  // Use just four by two armor texels (x20..23, y24..25) on the broad
  // faces: a few large highlight/shadow patches instead of the busy chest
  // pattern. One-texel top and side samples keep the rims clean and chunky.
  // Register these equipment textures as item-atlas sprites below. Using
  // vanilla resources also follows packs that recolor the vanilla armor.
  if(rank>=1) for(const e of elements) {
    const sidePocket=e.name.startsWith('side_pocket_');
    const canteen=e.name.startsWith('canteen_buckle');
    const outward=sidePocket?'west':canteen?'east':'north';
    const fastening=['upper_front_trim','upper_front_buckle','side_pocket_left_leather_band','side_pocket_left_leather_tab'].includes(e.name);
    // Netherite's outer chest edge is nearly black after world lighting.
    // Use the inner steel-gray patches to keep its chunky shading softer.
    const shadow=rank===5?[5.25,12,5.5,12.5]:[5,12,5.25,12.5];
    const facePatch=rank===5?[5.5,12,6.5,13]:[5,12,6,13];
    for(const [side,face] of Object.entries(e.faces)) {
      if(face.texture!=='#armor')continue;
      delete face.rotation;
      face.uv=e.name==='quiver_metal_accent'||side==='down'
        ? clone(shadow)
        : side==='up' ? [5.5,12,5.75,12.5]
        : fastening&&side!==outward ? clone(shadow)
        : clone(facePatch);
      if(fastening&&side===outward&&(e.name.endsWith('tab')||e.name.endsWith('buckle')))face.rotation=90;
    }
  }
  // Add 5% to the complete fabric body's height, anchored at the shoulders.
  // Extend only the lower body and long harness straps. Translate equipment,
  // pockets, feet and their details as complete assemblies to preserve sizes,
  // mounting joins and the tucked bedroll's spacing above the front pocket.
  const extraHeight=round((originalBody.to[1]-originalBody.from[1])*.05);
  for(const e of elements) {
    if(e.name==='main_body'||e.name==='left_outer_strap'||e.name==='right_outer_strap') {
      e.from[1]=round(e.from[1]-extraHeight);
    } else if(!/^(main_body_|handle_post_|carry_handle$|iron_padded_handle$|iron_handle_stitch_|lid_reinforcement$)/.test(e.name)) {
      e.from[1]=round(e.from[1]-extraHeight);
      e.to[1]=round(e.to[1]-extraHeight);
      if(e.rotation)e.rotation.origin[1]=round(e.rotation.origin[1]-extraHeight);
    }
  }
  // Display fitting follows the new silhouette; wearable scale stays separate.
  const display=clone(base.display);
  display.ground.translation=[0,rank>=3?4:2,0];
  const item={parent:'limesbackpacks:item/backpack_hiking',textures:{'0':'limesbackpacks:item/hiking_backpack',cloth:'limesbackpacks:item/expedition_soft',legacy:`limesbackpacks:item/${tier}_backpack`,equipment:'limesbackpacks:item/netherite_backpack',iron:'limesbackpacks:item/iron_backpack',leather:'limesbackpacks:item/leather_backpack',particle:'limesbackpacks:item/expedition_soft'},display,elements};
  if(rank>=1)item.textures.armor=`limesbackpacks:item/armor_metal_${tier}`;
  if(rank===3)item.textures.bedroll='limesbackpacks:item/sleeping_bag_variants';
  if(rank===4)item.textures.bedroll='limesbackpacks:item/sleeping_bag_oatmeal';
  if(rank>=4)item.textures.lantern='limesbackpacks:item/vanilla_lantern';
  fitGui(item);
  fitFixed(item);
  fitThirdPersonHand(item);
  const worn=wornModel(item);
  fitGui(worn);
  fitFixed(worn);
  fitThirdPersonHand(worn);
  files[`src/main/resources/assets/limesbackpacks/models/item/${tier}_backpack.json`]=JSON.stringify(item,null,2)+'\n';
  files[`src/main/resources/assets/limesbackpacks/models/item/${tier}_backpack_worn.json`]=JSON.stringify(worn,null,2)+'\n';
  if(rank===5){
    const emptyQuiver=clone(worn);
    emptyQuiver.elements=emptyQuiver.elements.filter(e=>!e.name.startsWith('arrow_'));
    files['src/main/resources/assets/limesbackpacks/models/item/netherite_backpack_empty_quiver.json']=JSON.stringify(emptyQuiver,null,2)+'\n';
    files['src/main/resources/assets/limesbackpacks/items/netherite_backpack_empty_quiver.json']=JSON.stringify({model:{type:'minecraft:model',model:'limesbackpacks:item/netherite_backpack_empty_quiver'}},null,2)+'\n';
  }
  // Every in-game display uses the corrected layout, including GUI, hands,
  // ground and frames. Keep the source mesh for the established preview view.
  files[`src/main/resources/assets/limesbackpacks/items/${tier}_backpack.json`]=JSON.stringify({model:{type:'minecraft:model',model:`limesbackpacks:item/${tier}_backpack_worn`}},null,2)+'\n';
  files[`src/main/resources/assets/limesbackpacks/items/${tier}_backpack_worn.json`]=JSON.stringify({model:{type:'minecraft:model',model:`limesbackpacks:item/${tier}_backpack_worn`}},null,2)+'\n';
  if(rank>=4) {
    for(const name of [tier+'_backpack', ...(rank===5?['netherite_backpack_empty_quiver']:[])]) {
      const source = name.endsWith('empty_quiver')
        ? JSON.parse(files['src/main/resources/assets/limesbackpacks/models/item/'+name+'.json']) : worn;
      files['src/main/resources/assets/limesbackpacks/models/item/'+name+'_unlit.json'] = JSON.stringify(require('./unlit-model.cjs')(source),null,2)+'\n';
      files['src/main/resources/assets/limesbackpacks/items/'+name+'_unlit.json'] = JSON.stringify({model:{type:'minecraft:model',model:'limesbackpacks:item/'+name+'_unlit'}},null,2)+'\n';
    }
  }
}
files['src/main/resources/assets/minecraft/atlases/items.json']=JSON.stringify({sources:tiers.slice(1).map(tier=>({
  type:'minecraft:single',resource:`minecraft:entity/equipment/humanoid/${tier}`,sprite:`limesbackpacks:item/armor_metal_${tier}`
}))},null,2)+'\n';
if(process.argv.includes('--write')) {
  for(const [relative,content] of Object.entries(files)) {
    const target=path.join(root,relative);
    // Preserve existing formatting and avoid touching unchanged variants.
    if(fs.existsSync(target)&&JSON.stringify(JSON.parse(fs.readFileSync(target,'utf8')))===JSON.stringify(JSON.parse(content)))continue;
    fs.mkdirSync(path.dirname(target),{recursive:true});
    fs.writeFileSync(target,content);
    console.log(relative);
  }
} else process.stdout.write(JSON.stringify(files));
