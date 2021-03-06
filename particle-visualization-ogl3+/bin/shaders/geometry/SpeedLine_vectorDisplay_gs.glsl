#version 330

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform int renderMode;
uniform vec4 globalColor;
uniform vec2 screenSize;
uniform float spriteSize;

layout (lines) in;
layout (line_strip, max_vertices = 8) out;

out vec3 normal;
out vec4 color;


//mat4 mvpMatrix = projectionMatrix * viewMatrix * modelMatrix;
// MS
//   modelMatrix
// WS
//   viewMatrix
// CS
//   projectionMatrix
// SS


void emitVertexCamspace(vec4 position){
	position = projectionMatrix * position;
	gl_Position = position;
	EmitVertex();
}
void emitVertexWorldspace(vec4 position){
	position = viewMatrix * position;
	emitVertexCamspace(position);
}
void emitVertexModelspace(vec4 position){
	position = modelMatrix * position;
	emitVertexWorldspace(position);
}
vec4 normalCross(vec4 v1, vec4 v2){
	return vec4(normalize(cross(v1.xyz, v2.xyz)), 1);
}
vec4 normalCrossSpriteScaled(vec4 v1, vec4 v2){
	return 0.5 * spriteSize * normalCross(v1, v2);
}

void main(void) {
	// using world coords for calc...
	//vec4 camPos = vec4(0, 0, 0, 0);
	vec4 pos0 = viewMatrix * modelMatrix * gl_in[0].gl_Position;
	vec4 pos1 = viewMatrix * modelMatrix * gl_in[1].gl_Position;
	vec4 lineVector = pos1 - pos0;
    
    vec4 camPos = vec4(0, 0, 0, 1);
	vec4 pos0ToCamVector = camPos - pos0;
	
	
	// color = vec4(0,0,1,0.5);
	// emitVertexWorldspace(pos0);
	// color = vec4(0,1,0,0.5);
	// emitVertexWorldspace(pos1);
    // EndPrimitive();
    
   
 	// color = vec4(0.2,0,0,1);
	// emitVertexWorldspace(pos0);
	// emitVertexWorldspace(pos0 + pos0ToCamVector);
    // EndPrimitive();
    
 	color = vec4(0.5,0,0.4,1);
	emitVertexCamspace(pos0);
	emitVertexCamspace(pos0 + normalCrossSpriteScaled(pos0ToCamVector,lineVector));
    EndPrimitive();
    
 	color = vec4(0.5,0.4,0,1);
	emitVertexCamspace(pos0);
	emitVertexCamspace(pos0 + normalCrossSpriteScaled(lineVector,pos0ToCamVector));
    EndPrimitive();
    
 	color = vec4(0,0,1,1);
	emitVertexCamspace(pos0);
	emitVertexCamspace(pos0 + lineVector);
    EndPrimitive();
}



