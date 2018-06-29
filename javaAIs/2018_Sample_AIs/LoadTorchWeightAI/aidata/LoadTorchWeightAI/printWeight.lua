require 'cutorch'
require 'cunn'
require 'io'

FilePath = './FightingICE/data/aiData/LoadTorch/Sample.dat'
model=torch.load(FilePath)
model = model:cuda()

print(model)

splitter = ","

for n=1,model:size() do
	if torch.isTypeOf(model:get(n),nn.Linear) then
		print("model:get("..n..")["..torch.typename(model:get(n)).."] is Linear type")
		local weight = model:get(n).weight
		filename = "./model_weight"..n..".csv"
		local out = assert(io.open(filename, "w")) -- open a file for serialization
		print(weight:size(1).."*"..weight:size(2))
		for i=1,weight:size(1) do
		    if i%100==1 then
		    	print("procss_weight ["..n.."/"..model:size().."]:"..i.."/"..weight:size(1))
		    end
		    for j=1,weight:size(2) do
			out:write(weight[i][j])
			if j == weight:size(2) then
			    out:write("\n")
			else
			    out:write(splitter)
			end
		    end
		end
		out:close()
	else
		print("model:get("..n..") is not Linear type")
	end
end

for n=1,model:size() do
	if torch.isTypeOf(model:get(n),nn.Linear) then
		print("model:get("..n..") is Linear type")
		local out = assert(io.open("./model_bias"..n..".csv", "w")) -- open a file for serialization
		local bias = model:get(n).bias
		for i=1,bias:size(1) do
		    if i%100==1 then
		    	print("procss_bias["..n.."/"..model:size().."]:"..i.."/"..bias:size(1))
		    end
		    out:write(bias[i])
		    out:write(splitter)
		end
		out:close()
	else
		print("model:get("..n..") is not Linear type")
	end
end