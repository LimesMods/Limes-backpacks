// Use the same recessed window geometry for dark, non-emissive glass.
module.exports = function unlitModel(source) {
  const model = structuredClone(source);
  model.textures.unlit_glass = 'limesbackpacks:item/lantern_unlit_glass';
  for (const element of model.elements) {
    // The lit atlas paints warm copper/amber onto both the cage and the neck.
    // Reuse its existing grey metal base for these faces when extinguished.
    if (element.name === 'vanilla_lantern_0' || element.name === 'vanilla_lantern_1') {
      for (const side of ['north', 'south', 'east', 'west']) {
        const face = element.faces[side];
        const mirrored = face.uv[0] > face.uv[2];
        face.uv = mirrored ? [6, 9, 0, 15] : [0, 9, 6, 15];
      }
    }
    if (!element.name.startsWith('lantern_glow_')) continue;
    element.name = element.name.replace('lantern_glow_', 'lantern_unlit_');
    delete element.light_emission;
    element.shade = true;
    for (const face of Object.values(element.faces)) {
      face.texture = '#unlit_glass';
      face.uv = [0, 0, 16, 16];
    }
  }
  return model;
};
